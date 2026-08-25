package com.buscadorandroid.app.domain.repository

import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.FiltroBusqueda
import kotlinx.coroutines.flow.Flow

interface BusquedaRepository {
    /** Busca archivos que coincidan con el filtro (usa el índice si está disponible). */
    fun buscar(filtro: FiltroBusqueda): Flow<List<Archivo>>

    /** Lista todas las extensiones disponibles en el dispositivo (para autocompletar). */
    suspend fun obtenerExtensionesDisponibles(): Set<String>

    /** Guarda una búsqueda en el historial. */
    suspend fun guardarHistorial(texto: String)

    /** Devuelve las búsquedas recientes distintas. */
    suspend fun obtenerHistorial(): List<String>

    /** Elimina todo el historial. */
    suspend fun limpiarHistorial()

    /** Fuerza la reconstrucción del índice desde MediaStore. */
    suspend fun indexarAhora()

    /** Cantidad de archivos indexados (0 = índice vacío). */
    suspend fun contarIndice(): Int
}
