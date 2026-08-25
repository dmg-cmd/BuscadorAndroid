package com.buscadorandroid.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface ArchivoDao {
    /** Búsqueda con cláusulas dinámicas (WHERE/ORDER BY) construidas por el repositorio. */
    @RawQuery
    suspend fun buscarRaw(query: SupportSQLiteQuery): List<ArchivoIndexado>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(archivos: List<ArchivoIndexado>)

    @Query("DELETE FROM archivos_indexados")
    suspend fun limpiar()

    @Query("SELECT COUNT(*) FROM archivos_indexados")
    suspend fun contar(): Int
}
