package com.buscadorandroid.app.domain.model

/**
 * Criterios de ordenamiento de los resultados.
 */
enum class OrdenBusqueda(
    val etiqueta: String
) {
    RELEVANCIA("Relevancia"),
    NOMBRE("Nombre"),
    FECHA("Fecha"),
    TAMANO("Tamaño"),
    TIPO("Tipo");

    companion object {
        /** Convierte el enum a la cláusula ORDER BY de MediaStore. */
        fun aOrderBySql(orden: OrdenBusqueda): String {
            return when (orden) {
                NOMBRE -> "${MediaStoreRef.DISPLAY_NAME} ASC"
                FECHA -> "${MediaStoreRef.DATE_MODIFIED} DESC"
                TAMANO -> "${MediaStoreRef.SIZE} DESC"
                TIPO -> "${MediaStoreRef.MIME_TYPE} ASC"
                RELEVANCIA -> "${MediaStoreRef.DATE_MODIFIED} DESC"
            }
        }

        /** Convierte el enum a la cláusula ORDER BY de la tabla indexada (archivos_indexados). */
        fun aOrderBySqlIndice(orden: OrdenBusqueda): String {
            return when (orden) {
                NOMBRE -> "nombre ASC"
                FECHA -> "fechaModificacion DESC"
                TAMANO -> "tamanoBytes DESC"
                TIPO -> "tipo ASC"
                RELEVANCIA -> "fechaModificacion DESC"
            }
        }
    }

    /** Referencias a columnas de MediaStore para evitar acoplar el dominio a android.provider. */
    private object MediaStoreRef {
        const val DISPLAY_NAME = "display_name"
        const val DATE_MODIFIED = "date_modified"
        const val SIZE = "size"
        const val MIME_TYPE = "mime_type"
    }
}
