package com.buscadorandroid.app.domain.model

/**
 * Tipos de archivo con sus extensiones comunes.
 * Permite filtrar por tipo o por extensión específica.
 */
enum class TipoArchivo(
    val etiqueta: String,
    val extensiones: Set<String>,
    val descripcion: String
) {
    IMAGEN(
        etiqueta = "Imágenes",
        extensiones = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "raw", "tiff"),
        descripcion = "Fotos e imágenes"
    ),
    VIDEO(
        etiqueta = "Videos",
        extensiones = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v", "mpg", "mpeg"),
        descripcion = "Videos y películas"
    ),
    AUDIO(
        etiqueta = "Música",
        extensiones = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "wma", "opus", "mid", "midi", "amr"),
        descripcion = "Música y audios"
    ),
    DOCUMENTO(
        etiqueta = "Documentos",
        extensiones = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv"),
        descripcion = "Documentos de texto y oficina"
    ),
    APK(
        etiqueta = "APKs",
        extensiones = setOf("apk", "xapk", "apks"),
        descripcion = "Instaladores de apps"
    ),
    COMPRIMIDO(
        etiqueta = "Comprimidos",
        extensiones = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz"),
        descripcion = "Archivos comprimidos"
    ),
    OTRO(
        etiqueta = "Otros",
        extensiones = emptySet(),
        descripcion = "Otros archivos"
    );

    companion object {
        // Mapa extensión -> tipo para búsqueda rápida
        private val mapaExtensionATipo: Map<String, TipoArchivo> by lazy {
            val mapa = mutableMapOf<String, TipoArchivo>()
            entries.filter { it != OTRO }.forEach { tipo ->
                tipo.extensiones.forEach { ext ->
                    mapa[ext.lowercase()] = tipo
                }
            }
            mapa
        }

        /** Obtiene el tipo a partir de una extensión (sin punto, ej: "jpg") */
        fun desdeExtension(extension: String): TipoArchivo {
            return mapaExtensionATipo[extension.lowercase().trimStart('.')] ?: OTRO
        }

        /** Lista todas las extensiones conocidas (para autocompletar) */
        fun todasLasExtensiones(): Set<String> = mapaExtensionATipo.keys

        /** Busca tipos que contienen una extensión parcial (ej: "mp" -> mp3, mp4) */
        fun buscarPorExtensionParcial(query: String): Set<TipoArchivo> {
            val q = query.lowercase().trimStart('.')
            return entries.filter { tipo ->
                tipo.extensiones.any { it.contains(q) }
            }.toSet()
        }
    }
}
