package com.buscadorandroid.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro de búsquedas realizadas por el usuario para mostrar historial y sugerencias.
 */
@Entity(tableName = "historial_busqueda")
data class HistorialBusqueda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "texto") val texto: String,
    @ColumnInfo(name = "fecha") val fecha: Long = System.currentTimeMillis()
)
