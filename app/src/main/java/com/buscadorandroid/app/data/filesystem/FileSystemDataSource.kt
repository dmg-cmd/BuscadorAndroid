package com.buscadorandroid.app.data.filesystem

import android.net.Uri
import android.os.Environment
import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.FiltroBusqueda
import com.buscadorandroid.app.domain.model.OrdenBusqueda
import com.buscadorandroid.app.domain.model.TipoArchivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recorrido directo del sistema de archivos.
 * Solo funciona con MANAGE_EXTERNAL_STORAGE concedido.
 */
@Singleton
class FileSystemDataSource @Inject constructor() {

    suspend fun buscar(filtro: FiltroBusqueda): List<Archivo> = withContext(Dispatchers.IO) {
        val raiz = Environment.getExternalStorageDirectory() ?: return@withContext emptyList()
        if (!raiz.canRead()) return@withContext emptyList()

        val resultados = mutableListOf<Archivo>()
        val queryNorm = filtro.queryNormalizada
        val extensionesFiltro = filtro.extensionesEfectivas

        fun recorrer(directorio: File) {
            if (resultados.size >= 2000) return
            val archivos = try { directorio.listFiles() } catch (_: SecurityException) { null } ?: return
            for (archivo in archivos) {
                if (archivo.isDirectory) {
                    // Evitar carpetas del sistema muy profundas y ocultas innecesarias
                    if (archivo.name.startsWith(".")) continue
                    if (archivo.name == "Android" && archivo.parent == raiz.absolutePath) {
                        // Permitir pero no profundizar demasiado en Android/data
                        continue
                    }
                    recorrer(archivo)
                } else {
                    val nombre = archivo.name
                    if (nombre.isBlank()) continue
                    val nombreNorm = FiltroBusqueda.normalizar(nombre)
                    val coincideNombre = queryNorm.isEmpty() || nombreNorm.contains(queryNorm)

                    // Búsqueda por contenido: solo si está activa y el nombre no coincide
                    val coincideContenido = !coincideNombre && filtro.buscarEnContenido
                            && queryNorm.length >= 2
                            && coincideEnContenido(archivo, queryNorm)

                    if (!coincideNombre && !coincideContenido) continue

                    val extension = if ('.' in nombre) nombre.substringAfterLast('.', "").lowercase().trim() else ""
                    if (extensionesFiltro.isNotEmpty() && extension !in extensionesFiltro) continue

                    val tamano = archivo.length()
                    if (filtro.tamanoMinBytes != null && tamano < filtro.tamanoMinBytes) continue
                    if (filtro.tamanoMaxBytes != null && tamano > filtro.tamanoMaxBytes) continue

                    val fecha = archivo.lastModified()
                    if (filtro.fechaDesde != null && fecha < filtro.fechaDesde) continue
                    if (filtro.fechaHasta != null && fecha > filtro.fechaHasta) continue

                    val tipo = TipoArchivo.desdeExtension(extension)
                    resultados.add(
                        Archivo(
                            id = archivo.absolutePath.hashCode().toLong(),
                            nombre = nombre,
                            nombreNormalizado = nombreNorm,
                            ruta = archivo.absolutePath,
                            uri = Uri.fromFile(archivo),
                            tamanoBytes = tamano,
                            fechaModificacion = fecha,
                            mimeType = null,
                            tipo = tipo,
                            extension = extension
                        )
                    )
                }
            }
        }
        recorrer(raiz)

        // Ordenamiento según el criterio seleccionado
        resultados.sortWith(
            when (filtro.orden) {
                OrdenBusqueda.NOMBRE -> compareBy { it.nombre.lowercase() }
                OrdenBusqueda.FECHA -> compareByDescending { it.fechaModificacion }
                OrdenBusqueda.TAMANO -> compareByDescending { it.tamanoBytes }
                OrdenBusqueda.TIPO -> compareBy({ it.tipo.ordinal }, { it.nombre.lowercase() })
                OrdenBusqueda.RELEVANCIA -> compareBy(
                    { !it.nombreNormalizado.startsWith(queryNorm) },
                    { !it.nombreNormalizado.contains(queryNorm) },
                    { it.nombre }
                )
            }
        )
        resultados
    }

    /** Extensiones consideradas texto plano para búsqueda por contenido. */
    private val extensionesTexto = setOf(
        "txt", "csv", "log", "md", "json", "xml", "html", "htm", "ini", "cfg",
        "java", "kt", "kts", "py", "js", "ts", "css", "sql", "yml", "yaml",
        "properties", "gradle", "sh", "bat", "ps1", "c", "cpp", "h", "hpp", "cs"
    )

    /** Lee el archivo (si es texto y pequeño) y comprueba si contiene el texto normalizado. */
    private fun coincideEnContenido(archivo: File, queryNorm: String): Boolean {
        val extension = archivo.extension.lowercase()
        if (extension !in extensionesTexto) return false
        if (archivo.length() > 2 * 1024 * 1024) return false // límite 2 MB
        return try {
            archivo.bufferedReader(Charsets.UTF_8).useLines { lineas ->
                lineas.any { FiltroBusqueda.normalizar(it).contains(queryNorm) }
            }
        } catch (_: Exception) {
            false
        }
    }
}
