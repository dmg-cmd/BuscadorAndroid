package com.buscadorandroid.app.domain.model

import java.text.Normalizer

/**
 * Filtro de búsqueda. Combina texto + tipo + extensión.
 *
 * El usuario puede:
 * - Escribir texto libre (busca en nombre)
 * - Seleccionar uno o varios TipoArchivo (Imágenes, Música, etc.)
 * - Escribir/seleccionar extensiones específicas (ej: "pdf", "mp3")
 *
 * Si se seleccionan tipos Y extensiones, se aplica OR entre ambos criterios.
 */
data class FiltroBusqueda(
    val query: String = "",
    val tiposSeleccionados: Set<TipoArchivo> = emptySet(),
    val extensionesSeleccionadas: Set<String> = emptySet(), // sin punto, lowercase ej: "pdf"
    val tamanoMinBytes: Long? = null,
    val tamanoMaxBytes: Long? = null,
    val fechaDesde: Long? = null,
    val fechaHasta: Long? = null,
    val usarRegex: Boolean = false,
    val orden: OrdenBusqueda = OrdenBusqueda.RELEVANCIA,
    val buscarEnContenido: Boolean = false
) {
    /** Normaliza query para comparar sin acentos y sin mayúsculas */
    val queryNormalizada: String
        get() = normalizar(query)

    /** Todas las extensiones efectivas: unión de tipos + extensiones manuales (cualquier extensión válida) */
    val extensionesEfectivas: Set<String>
        get() {
            val porTipo = tiposSeleccionados.flatMap { it.extensiones }.map { it.lowercase() }.toSet()
            // Normaliza cada extensión manual: quita puntos, espacios, lower, acepta CUALQUIER extensión
            val manuales = extensionesSeleccionadas
                .map { it.lowercase().trim().trimStart('.').trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            return porTipo + manuales
        }

    /** Indica si hay algún filtro activo además del texto */
    val tieneFiltrosActivos: Boolean
        get() = tiposSeleccionados.isNotEmpty() || extensionesSeleccionadas.isNotEmpty()
                || tamanoMinBytes != null || tamanoMaxBytes != null

    /** Verifica si un archivo pasa los filtros de tipo/extensión */
    fun coincideConFiltros(archivo: Archivo): Boolean {
        // Si no hay filtro de tipo/extensión, todo pasa
        if (extensionesEfectivas.isEmpty()) return true
        // Si el archivo es OTRO pero su extensión está en la lista manual, también pasa
        return archivo.extension.lowercase() in extensionesEfectivas
    }

    companion object {
        fun normalizar(texto: String): String {
            return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .trim()
        }

        /** Valida si una extensión es sintácticamente válida (cualquier extensión de 1-20 caracteres alfanuméricos) */
        fun esExtensionValida(extension: String): Boolean {
            val limpia = extension.lowercase().trim().trimStart('.').trim()
            if (limpia.isEmpty() || limpia.length > 20) return false
            return limpia.matches(Regex("^[a-z0-9]+$"))
        }

        /** Detecta si el texto parece una extensión suelta (ej: ".pdf", "*.log", "ext:dwg", ".bak") */
        fun detectarExtensionEnQuery(texto: String): String? {
            val t = texto.trim().lowercase()
            val regex = Regex("""^(?:ext:)?(?:\*\.)?\.?\s*([a-z0-9]{1,20})$""")
            val match = regex.find(t) ?: return null
            val ext = match.groupValues[1]
            if (t.startsWith(".") || t.startsWith("*.") || t.startsWith("ext:")) return ext
            return null
        }
    }
}
