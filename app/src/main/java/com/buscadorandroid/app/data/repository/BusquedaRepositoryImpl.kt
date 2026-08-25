package com.buscadorandroid.app.data.repository

import android.net.Uri
import android.os.Environment
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.buscadorandroid.app.data.filesystem.FileSystemDataSource
import com.buscadorandroid.app.data.local.ArchivoDao
import com.buscadorandroid.app.data.local.ArchivoIndexado
import com.buscadorandroid.app.data.local.HistorialBusqueda
import com.buscadorandroid.app.data.local.HistorialDao
import com.buscadorandroid.app.data.mediastore.MediaStoreDataSource
import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.FiltroBusqueda
import com.buscadorandroid.app.domain.model.OrdenBusqueda
import com.buscadorandroid.app.domain.model.TipoArchivo
import com.buscadorandroid.app.domain.repository.BusquedaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusquedaRepositoryImpl @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val fileSystem: FileSystemDataSource,
    private val archivoDao: ArchivoDao,
    private val historialDao: HistorialDao
) : BusquedaRepository {

    override fun buscar(filtro: FiltroBusqueda): Flow<List<Archivo>> = flow {
        // Si la query está vacía y no hay filtros, no buscar (evitar listar todo)
        if (filtro.query.isBlank() && !filtro.tieneFiltrosActivos) {
            emit(emptyList())
            return@flow
        }

        // Estrategia: si el índice tiene datos, usarlo (instantáneo); sino búsqueda en vivo.
        val hayIndice = try { archivoDao.contar() > 0 } catch (_: Exception) { false }

        val resultados = if (hayIndice) {
            try {
                val query = construirQueryIndice(filtro)
                archivoDao.buscarRaw(query).map { it.aArchivo() }
            } catch (_: Exception) {
                buscarEnVivo(filtro)
            }
        } else {
            buscarEnVivo(filtro)
        }

        // Actualiza el índice con lo encontrado para futuras búsquedas instantáneas
        if (resultados.isNotEmpty()) {
            try {
                archivoDao.insertarTodos(resultados.map { it.aIndice() })
            } catch (_: Exception) { }
        }

        emit(resultados)
    }

    /** Búsqueda en vivo (MediaStore + FileSystem con acceso total). */
    private suspend fun buscarEnVivo(filtro: FiltroBusqueda): List<Archivo> {
        val inicio = System.currentTimeMillis()
        val resultadosMedia = try {
            mediaStore.buscar(filtro)
        } catch (e: Exception) {
            emptyList()
        }

        val tieneAccesoTotal = Environment.isExternalStorageManager()
        val resultadosFinales = if (tieneAccesoTotal && resultadosMedia.size < 500) {
            try {
                val extra = fileSystem.buscar(filtro)
                val rutasVistas = resultadosMedia.map { it.ruta }.toSet()
                resultadosMedia + extra.filter { it.ruta !in rutasVistas }
            } catch (_: Exception) {
                resultadosMedia
            }
        } else {
            resultadosMedia
        }
        return resultadosFinales
    }

    override suspend fun obtenerExtensionesDisponibles(): Set<String> {
        return try {
            val filtroVacio = FiltroBusqueda(query = "a")
            val muestra = mediaStore.buscar(filtroVacio).take(500)
            val reales = muestra.mapNotNull { it.extension.takeIf { e -> e.isNotBlank() } }.toSet()
            if (reales.isNotEmpty()) reales + TipoArchivo.todasLasExtensiones()
            else TipoArchivo.todasLasExtensiones()
        } catch (_: Exception) {
            TipoArchivo.todasLasExtensiones()
        }
    }

    override suspend fun guardarHistorial(texto: String) {
        val limpio = texto.trim()
        if (limpio.isBlank()) return
        try {
            historialDao.insertar(HistorialBusqueda(texto = limpio))
        } catch (_: Exception) { }
    }

    override suspend fun obtenerHistorial(): List<String> {
        return try {
            historialDao.obtenerRecientes(12).map { it.texto }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun limpiarHistorial() {
        try { historialDao.limpiar() } catch (_: Exception) { }
    }

    override suspend fun indexarAhora() {
        try {
            val lista = mediaStore.listarTodos()
            archivoDao.limpiar()
            if (lista.isNotEmpty()) archivoDao.insertarTodos(lista.map { it.aIndice() })
        } catch (_: Exception) { }
    }

    override suspend fun contarIndice(): Int {
        return try { archivoDao.contar() } catch (_: Exception) { 0 }
    }

    /** Construye la consulta SQL del índice según el filtro. */
    private fun construirQueryIndice(filtro: FiltroBusqueda): SupportSQLiteQuery {
        val condiciones = mutableListOf<String>()
        val argumentos = mutableListOf<Any>()

        val q = filtro.queryNormalizada
        if (q.isNotEmpty()) {
            condiciones.add("nombreNormalizado LIKE ?")
            argumentos.add("%$q%")
        }
        val extensiones = filtro.extensionesEfectivas
        if (extensiones.isNotEmpty()) {
            val marcadores = extensiones.joinToString(", ") { "?" }
            condiciones.add("extension IN ($marcadores)")
            argumentos.addAll(extensiones)
        }
        filtro.tamanoMinBytes?.let {
            condiciones.add("tamanoBytes >= ?"); argumentos.add(it)
        }
        filtro.tamanoMaxBytes?.let {
            condiciones.add("tamanoBytes <= ?"); argumentos.add(it)
        }
        filtro.fechaDesde?.let {
            condiciones.add("fechaModificacion >= ?"); argumentos.add(it)
        }
        filtro.fechaHasta?.let {
            condiciones.add("fechaModificacion <= ?"); argumentos.add(it)
        }

        val where = if (condiciones.isEmpty()) null else condiciones.joinToString(" AND ")
        val orderBy = OrdenBusqueda.aOrderBySqlIndice(filtro.orden)
        return SupportSQLiteQueryBuilder.builder("archivos_indexados")
            .selection(where, argumentos.toTypedArray())
            .orderBy(orderBy)
            .create()
    }
}

// Conversores entre el modelo de dominio y la entidad indexada
private fun Archivo.aIndice(): ArchivoIndexado = ArchivoIndexado(
    id = id,
    nombre = nombre,
    nombreNormalizado = nombreNormalizado,
    ruta = ruta,
    extension = extension,
    tipo = tipo.name,
    tamanoBytes = tamanoBytes,
    fechaModificacion = fechaModificacion,
    uri = uri.toString()
)

private fun ArchivoIndexado.aArchivo(): Archivo = Archivo(
    id = id,
    nombre = nombre,
    nombreNormalizado = nombreNormalizado,
    ruta = ruta,
    uri = Uri.parse(uri),
    tamanoBytes = tamanoBytes,
    fechaModificacion = fechaModificacion,
    mimeType = null,
    tipo = try { TipoArchivo.valueOf(tipo) } catch (_: Exception) { TipoArchivo.OTRO },
    extension = extension
)
