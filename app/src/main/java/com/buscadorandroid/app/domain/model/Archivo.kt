package com.buscadorandroid.app.domain.model

import android.net.Uri
import java.io.File

/**
 * Modelo de dominio que representa un archivo encontrado.
 */
data class Archivo(
    val id: Long,
    val nombre: String,
    val nombreNormalizado: String, // lowerCase sin acentos para búsqueda
    val ruta: String,
    val uri: Uri,
    val tamanoBytes: Long,
    val fechaModificacion: Long,
    val mimeType: String?,
    val tipo: TipoArchivo,
    val extension: String
) {
    /** Tamaño legible: "2.5 MB", "340 KB" */
    val tamanoLegible: String
        get() = formatearTamano(tamanoBytes)

    /** Ruta completa de la carpeta que contiene el archivo */
    val carpetaContenedora: String
        get() {
            if (ruta.isNotBlank()) {
                val f = File(ruta)
                val p = f.parent
                if (!p.isNullOrBlank()) return p
            }
            return "/storage/emulated/0"
        }

    /** Nombre corto y legible de la carpeta (ej. "DCIM/Camera", "Download", "WhatsApp Images") */
    val nombreCarpetaSimple: String
        get() {
            val c = carpetaContenedora
            val limpia = c.removePrefix("/storage/emulated/0/").removePrefix("/storage/emulated/0")
            if (limpia.isBlank() || limpia == "/") return "Almacenamiento interno (raíz)"
            return limpia.trimStart('/')
        }

    companion object {
        fun formatearTamano(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format("%.2f GB", gb)
        }
    }
}
