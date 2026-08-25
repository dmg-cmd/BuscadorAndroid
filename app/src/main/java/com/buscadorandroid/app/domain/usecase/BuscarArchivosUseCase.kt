package com.buscadorandroid.app.domain.usecase

import com.buscadorandroid.app.domain.model.FiltroBusqueda
import com.buscadorandroid.app.domain.repository.BusquedaRepository
import javax.inject.Inject

class BuscarArchivosUseCase @Inject constructor(
    private val repositorio: BusquedaRepository
) {
    operator fun invoke(filtro: FiltroBusqueda) = repositorio.buscar(filtro)
}
