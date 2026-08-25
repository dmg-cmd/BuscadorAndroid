package com.buscadorandroid.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ArchivoIndexado::class, HistorialBusqueda::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun archivoDao(): ArchivoDao
    abstract fun historialDao(): HistorialDao
}
