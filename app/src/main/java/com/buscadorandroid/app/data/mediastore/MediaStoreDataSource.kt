package com.buscadorandroid.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.FiltroBusqueda
import com.buscadorandroid.app.domain.model.OrdenBusqueda
import com.buscadorandroid.app.domain.model.TipoArchivo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val contexto: Context
) {

    suspend fun buscar(filtro: FiltroBusqueda): List<Archivo> = withContext(Dispatchers.IO) {
        val resultados = mutableListOf<Archivo>()
        val queryNorm = filtro.queryNormalizada
        val extensionesFiltro = filtro.extensionesEfectivas

        // Proyección mínima necesaria
        val proyeccion = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA, // ruta (deprecated pero útil)
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            "relative_path",
            "bucket_display_name"
        )

        val uriColeccion = MediaStore.Files.getContentUri("external")

        // No filtramos en SQL por query para permitir búsqueda sin acentos y contains
        val ordenSql = OrdenBusqueda.aOrderBySql(filtro.orden)
        val cursor = contexto.contentResolver.query(
            uriColeccion,
            proyeccion,
            null,
            null,
            ordenSql
        ) ?: return@withContext resultados

        cursor.use {
            val idxId = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val idxNombre = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val idxData = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val idxTamano = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val idxFecha = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val idxMime = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val idxRelative = it.getColumnIndex("relative_path")
            val idxBucket = it.getColumnIndex("bucket_display_name")

            while (it.moveToNext()) {
                val id = it.getLong(idxId)
                val nombre = it.getString(idxNombre) ?: continue
                if (nombre.isBlank()) continue

                val nombreNorm = FiltroBusqueda.normalizar(nombre)

                // Filtro por texto (contains sin acentos)
                if (queryNorm.isNotEmpty() && !nombreNorm.contains(queryNorm)) {
                    continue
                }

                // Extraer extensión - soporta CUALQUIER extensión (log, tmp, bak, dwg, etc.), no solo las comunes
                val extension = if ('.' in nombre) nombre.substringAfterLast('.', "").lowercase().trim() else ""
                val tipo = TipoArchivo.desdeExtension(extension)

                // Filtro por tipo/extensión
                if (extensionesFiltro.isNotEmpty() && extension !in extensionesFiltro) {
                    continue
                }

                // Filtro por tamaño
                val tamano = it.getLong(idxTamano)
                if (filtro.tamanoMinBytes != null && tamano < filtro.tamanoMinBytes) continue
                if (filtro.tamanoMaxBytes != null && tamano > filtro.tamanoMaxBytes) continue

                val fechaSeg = it.getLong(idxFecha)
                val fechaMs = fechaSeg * 1000
                if (filtro.fechaDesde != null && fechaMs < filtro.fechaDesde) continue
                if (filtro.fechaHasta != null && fechaMs > filtro.fechaHasta) continue

                var ruta = if (idxData != -1) it.getString(idxData) ?: "" else ""
                if (ruta.isBlank()) {
                    val relative = if (idxRelative != -1) it.getString(idxRelative) ?: "" else ""
                    val bucket = if (idxBucket != -1) it.getString(idxBucket) ?: "" else ""
                    ruta = when {
                        relative.isNotBlank() -> "/storage/emulated/0/${relative.trim('/')}/$nombre"
                        bucket.isNotBlank() -> "/storage/emulated/0/$bucket/$nombre"
                        else -> ""
                    }
                }
                val uriArchivo = ContentUris.withAppendedId(uriColeccion, id)
                val mime = it.getString(idxMime)

                resultados.add(
                    Archivo(
                        id = id,
                        nombre = nombre,
                        nombreNormalizado = nombreNorm,
                        ruta = ruta,
                        uri = uriArchivo,
                        tamanoBytes = tamano,
                        fechaModificacion = fechaMs,
                        mimeType = mime,
                        tipo = tipo,
                        extension = extension
                    )
                )

                // Limitar a 2000 resultados para no saturar UI; con paging se paginará
                if (resultados.size >= 2000) break
            }
        }
        // Ordenar por relevancia si hay query y el criterio es RELEVANCIA
        if (queryNorm.isNotEmpty() && filtro.orden == OrdenBusqueda.RELEVANCIA) {
            resultados.sortWith(compareBy(
                { !it.nombreNormalizado.startsWith(queryNorm) },
                { !it.nombreNormalizado.contains(queryNorm) },
                { it.nombre }
            ))
        }
        resultados
    }

    /**
     * Devuelve todos los archivos multimedia del MediaStore (para poblar el índice).
     * No aplica filtro de texto; usa un tope alto para no saturar.
     */
    suspend fun listarTodos(): List<Archivo> = withContext(Dispatchers.IO) {
        val resultados = mutableListOf<Archivo>()
        val proyeccion = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            "relative_path",
            "bucket_display_name"
        )
        val uriColeccion = MediaStore.Files.getContentUri("external")
        val cursor = contexto.contentResolver.query(
            uriColeccion, proyeccion, null, null,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        ) ?: return@withContext resultados

        cursor.use {
            val idxId = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val idxNombre = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val idxData = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val idxTamano = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val idxFecha = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val idxMime = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val idxRelative = it.getColumnIndex("relative_path")
            val idxBucket = it.getColumnIndex("bucket_display_name")

            while (it.moveToNext()) {
                val id = it.getLong(idxId)
                val nombre = it.getString(idxNombre) ?: continue
                if (nombre.isBlank()) continue
                val nombreNorm = FiltroBusqueda.normalizar(nombre)
                val extension = if ('.' in nombre) nombre.substringAfterLast('.', "").lowercase().trim() else ""
                val tipo = TipoArchivo.desdeExtension(extension)

                val tamano = it.getLong(idxTamano)
                val fechaSeg = it.getLong(idxFecha)
                val fechaMs = fechaSeg * 1000

                var ruta = if (idxData != -1) it.getString(idxData) ?: "" else ""
                if (ruta.isBlank()) {
                    val relative = if (idxRelative != -1) it.getString(idxRelative) ?: "" else ""
                    val bucket = if (idxBucket != -1) it.getString(idxBucket) ?: "" else ""
                    ruta = when {
                        relative.isNotBlank() -> "/storage/emulated/0/${relative.trim('/')}/$nombre"
                        bucket.isNotBlank() -> "/storage/emulated/0/$bucket/$nombre"
                        else -> ""
                    }
                }
                val uriArchivo = ContentUris.withAppendedId(uriColeccion, id)
                val mime = it.getString(idxMime)

                resultados.add(
                    Archivo(
                        id = id,
                        nombre = nombre,
                        nombreNormalizado = nombreNorm,
                        ruta = ruta,
                        uri = uriArchivo,
                        tamanoBytes = tamano,
                        fechaModificacion = fechaMs,
                        mimeType = mime,
                        tipo = tipo,
                        extension = extension
                    )
                )
                if (resultados.size >= 20000) break
            }
        }
        resultados
    }
}
