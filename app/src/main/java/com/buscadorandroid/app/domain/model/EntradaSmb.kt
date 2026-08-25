package com.buscadorandroid.app.domain.model

/**
 * Representa un archivo o carpeta dentro del share MiNube.
 */
data class EntradaSmb(
    val nombre: String,
    val ruta: String,           // ruta relativa dentro del share (sin host)
    val esDirectorio: Boolean,
    val tamanoBytes: Long,
    val fechaModificacion: Long
)
