package com.buscadorandroid.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistorialDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(historial: HistorialBusqueda)

    /** Últimas búsquedas distintas, de la más reciente a la más antigua. */
    @Query("SELECT * FROM historial_busqueda GROUP BY texto ORDER BY MAX(fecha) DESC LIMIT :limite")
    suspend fun obtenerRecientes(limite: Int = 12): List<HistorialBusqueda>

    @Query("DELETE FROM historial_busqueda")
    suspend fun limpiar()
}
