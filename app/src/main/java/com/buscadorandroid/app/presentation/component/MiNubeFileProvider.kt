package com.buscadorandroid.app.presentation.component

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

/**
 * Proveedor de contenido propio para previsualizar/abrir archivos de MiNube con
 * la aplicación del sistema (galería, reproductor de vídeo/música, etc.).
 *
 * No depende de un recurso res/xml (FileProvider), sino que sirve directamente
 * el archivo descargado en la caché de la app, evitando problemas de empaquetado
 * de recursos xml en el APK.
 *
 * Ruta: content://<applicationId>.minubefiles/<nombreSeguro>
 * El archivo real está en: cacheDir/minube_preview/<nombreSeguro>
 */
class MiNubeFileProvider : ContentProvider() {

    companion object {
        /** Autoridad del proveedor (applicationId + sufijo). */
        fun autoridad(ctx: Context): String = ctx.packageName + ".minubefiles"

        /** Construye el content Uri para un archivo ya descargado en la caché. */
        fun uriPara(ctx: Context, nombreSeguro: String): Uri =
            Uri.parse("content://" + autoridad(ctx) + "/" + nombreSeguro)
    }

    private val carpetaPreview: String = "minube_preview"

    override fun onCreate(): Boolean = true

    private fun archivoDe(uri: Uri): File? {
        val ctx = context ?: return null
        val nombre = uri.lastPathSegment ?: return null
        val archivo = File(File(ctx.cacheDir, carpetaPreview), nombre)
        return if (archivo.isFile) archivo else null
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val archivo = archivoDe(uri) ?: return null
        return ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = mimeDesdeNombre(uri.lastPathSegment ?: "")

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val columnas = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(columnas)
        val archivo = archivoDe(uri)
        cursor.addRow(
            columnas.map { col ->
                when (col) {
                    OpenableColumns.DISPLAY_NAME -> archivo?.name ?: uri.lastPathSegment
                    OpenableColumns.SIZE -> archivo?.length() ?: 0
                    else -> null
                }
            }
        )
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    private fun mimeDesdeNombre(nombre: String): String {
        val punto = nombre.lastIndexOf('.')
        if (punto < 0) return "application/octet-stream"
        val ext = nombre.substring(punto + 1).lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/x-wav"
            "ogg", "oga" -> "audio/ogg"
            "m4a", "aac" -> "audio/mp4"
            "flac" -> "audio/flac"
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "mkv" -> "video/x-matroska"
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
