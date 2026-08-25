package com.buscadorandroid.app.presentation.viewmodel

import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.FiltroBusqueda

data class BusquedaUiState(
    val filtro: FiltroBusqueda = FiltroBusqueda(),
    val resultados: List<Archivo> = emptyList(),
    val seleccionados: Set<Long> = emptySet(),
    val cargando: Boolean = false,
    val tiempoBusquedaMs: Long = 0,
    val mensajeError: String? = null,
    val mostrarSelectorTipo: Boolean = false
) {
    val haySeleccion: Boolean get() = seleccionados.isNotEmpty()
    val cantidadSeleccionados: Int get() = seleccionados.size
    val todosSeleccionados: Boolean get() = resultados.isNotEmpty() && seleccionados.size == resultados.size
}
