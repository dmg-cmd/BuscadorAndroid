package com.buscadorandroid.app.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.buscadorandroid.app.presentation.component.MiNubeFileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buscadorandroid.app.data.MinubeSettingsRepository
import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.EntradaSmb
import com.buscadorandroid.app.domain.model.MinubeConfig
import com.buscadorandroid.app.domain.repository.MinubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import javax.inject.Inject

enum class FaseMinube { SIN_CONFIG, CONECTANDO, CONECTADO, ERROR }

data class ProgresoMinube(val hecho: Long, val total: Long, val texto: String)

data class MinubeUiState(
    val fase: FaseMinube = FaseMinube.SIN_CONFIG,
    val rutaActual: String = "",
    val entradas: List<EntradaSmb> = emptyList(),
    val seleccion: Set<String> = emptySet(),
    val colaSubida: List<Archivo> = emptyList(),
    val progreso: ProgresoMinube? = null,
    val mensaje: String? = null,
    val buscando: Boolean = false,
    val cfg: MinubeConfig? = null
)

@HiltViewModel
class MinubeViewModel @Inject constructor(
    private val repositorio: MinubeRepository,
    private val ajustes: MinubeSettingsRepository,
    @ApplicationContext private val contexto: Context
) : ViewModel() {

    private val _estado = MutableStateFlow(MinubeUiState())
    val estado: StateFlow<MinubeUiState> = _estado.asStateFlow()

    private var cfgActual: MinubeConfig? = null
    private var terminoBusqueda: String = ""

    init {
        if (ajustes.hayConfig()) {
            val cfg = ajustes.obtener()
            _estado.value = _estado.value.copy(cfg = cfg)
            if (cfg != null) conectar(cfg)
        }
    }

    fun conectar(cfg: MinubeConfig) {
        cfgActual = cfg
        _estado.value = _estado.value.copy(fase = FaseMinube.CONECTANDO, cfg = cfg, mensaje = null)
        viewModelScope.launch(Dispatchers.IO) {
            repositorio.probarConexion(cfg)
                .onSuccess {
                    ajustes.guardar(cfg)
                    listar("")
                }
                .onFailure { e ->
                    _estado.value = _estado.value.copy(
                        fase = FaseMinube.ERROR,
                        mensaje = "No se pudo conectar: ${e.message}"
                    )
                }
        }
    }

    private fun listar(ruta: String) {
        terminoBusqueda = ""
        val cfg = cfgActual ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repositorio.listar(cfg, ruta)
                .onSuccess { lista ->
                    _estado.value = _estado.value.copy(
                        fase = FaseMinube.CONECTADO,
                        rutaActual = ruta,
                        entradas = lista,
                        seleccion = emptySet(),
                        mensaje = null,
                        buscando = false
                    )
                }
                .onFailure { e ->
                    _estado.value = _estado.value.copy(
                        fase = FaseMinube.ERROR,
                        mensaje = "Error al listar: ${e.message}"
                    )
                }
        }
    }

    fun entrarCarpeta(nombre: String) {
        val actual = _estado.value.rutaActual
        val nueva = if (actual.isBlank()) nombre else "${actual.trim('/')}/$nombre"
        listar(nueva)
    }

    /** Sube un nivel: vuelve a la carpeta superior dentro del recurso compartido. */
    fun subirNivel() {
        val actual = _estado.value.rutaActual
        if (actual.isBlank()) return
        val padre = actual.removeSuffix("/").substringBeforeLast('/', "")
        _estado.value = _estado.value.copy(
            rutaActual = padre,
            entradas = emptyList(),
            fase = FaseMinube.CONECTANDO,
            seleccion = emptySet(),
            mensaje = null
        )
        listar(padre)
    }

    /** Navega directamente a una ruta completa (usado al abrir un resultado de búsqueda). */
    fun irA(ruta: String) {
        terminoBusqueda = ""
        _estado.value = _estado.value.copy(
            rutaActual = ruta,
            entradas = emptyList(),
            fase = FaseMinube.CONECTANDO,
            seleccion = emptySet(),
            mensaje = null,
            buscando = false
        )
        listar(ruta)
    }

    /** Cambia el término de búsqueda: filtra en la carpeta actual y subcarpetas. */
    fun alCambiarBusqueda(texto: String) {
        terminoBusqueda = texto
        if (texto.isBlank()) {
            listar(_estado.value.rutaActual)
        } else {
            buscar(texto)
        }
    }

    private fun buscar(termino: String) {
        val cfg = cfgActual ?: return
        val base = _estado.value.rutaActual
        _estado.value = _estado.value.copy(
            buscando = true,
            entradas = emptyList(),
            fase = FaseMinube.CONECTANDO,
            seleccion = emptySet(),
            mensaje = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            repositorio.buscar(cfg, base, termino)
                .onSuccess { lista ->
                    _estado.value = _estado.value.copy(
                        fase = FaseMinube.CONECTADO,
                        entradas = lista,
                        mensaje = if (lista.isEmpty()) "Sin coincidencias para '$termino'" else null
                    )
                }
                .onFailure { e ->
                    _estado.value = _estado.value.copy(
                        fase = FaseMinube.CONECTADO,
                        mensaje = "Error al buscar: ${e.message}"
                    )
                }
        }
    }

    fun subirCola() {
        val cfg = cfgActual ?: return
        val cola = _estado.value.colaSubida
        if (cola.isEmpty()) return
        val ruta = _estado.value.rutaActual
        _estado.value = _estado.value.copy(
            progreso = ProgresoMinube(0, 0, "Subiendo ${cola.size} archivo(s)...")
        )
        viewModelScope.launch(Dispatchers.IO) {
            val semaforo = Semaphore(4)
            var fallos = 0
            cola.forEach { archivo ->
                semaforo.acquire()
                try {
                    contexto.contentResolver.openInputStream(archivo.uri)?.use { input ->
                        repositorio.subir(cfg, ruta, archivo.nombre, archivo.tamanoBytes, input)
                            .onFailure { fallos++ }
                    } ?: run { fallos++ }
                } catch (e: Exception) {
                    fallos++
                } finally {
                    semaforo.release()
                }
            }
            _estado.value = _estado.value.copy(
                colaSubida = emptyList(),
                progreso = null,
                mensaje = if (fallos == 0) "Subida completada" else "Subida terminada con $fallos error(es)"
            )
            listar(ruta)
        }
    }

    fun toggleSeleccion(ruta: String) {
        val sel = _estado.value.seleccion.toMutableSet()
        if (sel.contains(ruta)) sel.remove(ruta) else sel.add(ruta)
        _estado.value = _estado.value.copy(seleccion = sel)
    }

    fun descargar(treeUri: Uri) {
        val cfg = cfgActual ?: return
        val seleccionadas = _estado.value.entradas.filter {
            it.ruta in _estado.value.seleccion && !it.esDirectorio
        }
        if (seleccionadas.isEmpty()) return
        val tree = DocumentFile.fromTreeUri(contexto, treeUri) ?: return
        _estado.value = _estado.value.copy(
            progreso = ProgresoMinube(0, 0, "Descargando ${seleccionadas.size} archivo(s)...")
        )
        viewModelScope.launch(Dispatchers.IO) {
            var fallos = 0
            seleccionadas.forEach { entrada ->
                try {
                    val mime = when (entrada.nombre.substringAfterLast('.', "").lowercase()) {
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "mp4" -> "video/mp4"
                        "mp3" -> "audio/mpeg"
                        "pdf" -> "application/pdf"
                        else -> "application/octet-stream"
                    }
                    val dest = tree.createFile(mime, entrada.nombre) ?: return@forEach
                    contexto.contentResolver.openOutputStream(dest.uri)?.use { out ->
                        repositorio.descargar(cfg, entrada, out)
                            .onFailure { fallos++ }
                    } ?: run { fallos++ }
                } catch (e: Exception) {
                    fallos++
                }
            }
            _estado.value = _estado.value.copy(
                seleccion = emptySet(),
                progreso = null,
                mensaje = if (fallos == 0) "Descarga completada" else "Descarga terminada con $fallos error(es)"
            )
        }
    }

    fun crearCarpeta(nombre: String) {
        val cfg = cfgActual ?: return
        val ruta = _estado.value.rutaActual
        viewModelScope.launch(Dispatchers.IO) {
            repositorio.crearCarpeta(cfg, ruta, nombre)
                .onSuccess { listar(ruta) }
                .onFailure { e ->
                    _estado.value = _estado.value.copy(mensaje = "No se pudo crear: ${e.message}")
                }
        }
    }

    fun definirCola(archivos: List<Archivo>) {
        _estado.value = _estado.value.copy(colaSubida = archivos)
    }

    /**
     * Abre un archivo de la nube con la aplicación del sistema (galería, vídeo, música, etc.).
     * Lo descarga primero a la caché del teléfono y luego lanza un Intent ACTION_VIEW
     * con un content Uri de [MiNubeFileProvider], para no salir de la red local.
     */
    fun abrir(entrada: EntradaSmb) {
        val cfg = cfgActual ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _estado.value = _estado.value.copy(
                progreso = ProgresoMinube(0, 0, "Abriendo ${entrada.nombre}...")
            )
            val resultado = descargarArchivoTemporal(cfg, entrada)
            _estado.value = _estado.value.copy(progreso = null)
            resultado.onSuccess { archivo ->
                try {
                    val uri = MiNubeFileProvider.uriPara(contexto, archivo.name)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeDesde(entrada.nombre))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    contexto.startActivity(intent)
                } catch (e: Exception) {
                    _estado.value = _estado.value.copy(mensaje = "No se pudo abrir: ${e.message}")
                }
            }.onFailure { e ->
                _estado.value = _estado.value.copy(mensaje = "No se pudo abrir: ${e.message}")
            }
        }
    }

    private suspend fun descargarArchivoTemporal(
        cfg: MinubeConfig,
        entrada: EntradaSmb
    ): Result<File> = kotlin.runCatching {
        val dir = File(contexto.cacheDir, "minube_preview")
        dir.mkdirs()
        val nombreSeguro = entrada.nombre.replace(Regex("[^\\w.\\- ]"), "_")
        val destino = File(dir, nombreSeguro)
        destino.outputStream().use { salida ->
            repositorio.descargar(cfg, entrada, salida).getOrThrow()
        }
        destino
    }

    private fun mimeDesde(nombre: String): String {
        return when (nombre.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "mp4", "m4v", "3gp", "webm" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg", "oga" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
    }

    fun limpiarMensaje() {
        _estado.value = _estado.value.copy(mensaje = null)
    }
}
