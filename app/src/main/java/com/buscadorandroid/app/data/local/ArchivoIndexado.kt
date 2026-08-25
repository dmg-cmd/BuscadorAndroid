package com.buscadorandroid.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad indexada que actúa como caché rápida de los archivos del dispositivo.
 * Se consulta con LIKE/ORDER BY para búsquedas instantáneas sin recorrer el sistema.
 */
@Entity(
    tableName = "archivos_indexados",
    indices = [Index(value = ["nombreNormalizado"]), Index(value = ["extension"]), Index(value = ["tipo"])]
)
data class ArchivoIndexado(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "nombreNormalizado") val nombreNormalizado: String,
    @ColumnInfo(name = "ruta") val ruta: String,
    @ColumnInfo(name = "extension") val extension: String,
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "tamanoBytes") val tamanoBytes: Long,
    @ColumnInfo(name = "fechaModificacion") val fechaModificacion: Long,
    @ColumnInfo(name = "uri") val uri: String
)
