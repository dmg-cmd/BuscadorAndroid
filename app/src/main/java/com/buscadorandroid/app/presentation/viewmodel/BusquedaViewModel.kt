package com.buscadorandroid.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buscadorandroid.app.domain.model.FiltroBusqueda
import com.buscadorandroid.app.domain.model.OrdenBusqueda
import com.buscadorandroid.app.domain.model.TipoArchivo
import com.buscadorandroid.app.domain.repository.BusquedaRepository
import com.buscadorandroid.app.domain.usecase.BuscarArchivosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BusquedaViewModel @Inject constructor(
    private val buscarArchivos: BuscarArchivosUseCase,
    private val repositorio: BusquedaRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(BusquedaUiState())
    val estado: StateFlow<BusquedaUiState> = _estado.asStateFlow()

    private val _historial = MutableStateFlow<List<String>>(emptyList())
    val historial: StateFlow<List<String>> = _historial.asStateFlow()

    // Flujo interno para debounce de búsqueda
    private val filtroFlow = MutableStateFlow(FiltroBusqueda())

    init {
        viewModelScope.launch { _historial.value = repositorio.obtenerHistorial() }
        viewModelScope.launch {
            filtroFlow
                .debounce(350)
                .distinctUntilChanged()
                .collect { filtro ->
                    ejecutarBusqueda(filtro)
                }
        }
    }

    fun onQueryChange(nuevoQuery: String) {
        val nuevoFiltro = _estado.value.filtro.copy(query = nuevoQuery)
        _estado.update { it.copy(filtro = nuevoFiltro) }
        filtroFlow.value = nuevoFiltro
    }

    /** Alterna un tipo de archivo (Imágenes, Música, etc.) */
    fun toggleTipo(tipo: TipoArchivo) {
        val actuales = _estado.value.filtro.tiposSeleccionados
        val nuevos = if (tipo in actuales) actuales - tipo else actuales + tipo
        actualizarFiltro(_estado.value.filtro.copy(tiposSeleccionados = nuevos))
    }

    /** Agrega CUALQUIER extensión (ej: "pdf", "log", "dwg", "bak", "tmp") - sin restricción a lista conocida */
    fun agregarExtension(extension: String) {
        var ext = extension.lowercase().trim()
        ext = ext.removePrefix("*.").removePrefix("ext:").trimStart('.').trim()
        if (ext.isBlank() || ext.length > 20) return
        if (!ext.matches(Regex("^[a-z0-9]+$"))) return
        val actuales = _estado.value.filtro.extensionesSeleccionadas.map { it.lowercase().trimStart('.') }.toSet()
        if (ext in actuales) return
        actualizarFiltro(_estado.value.filtro.copy(extensionesSeleccionadas = actuales + ext))
    }

    fun quitarExtension(extension: String) {
        val ext = extension.lowercase().trim().trimStart('.').trim()
        val actuales = _estado.value.filtro.extensionesSeleccionadas.map { it.lowercase().trimStart('.') }.toSet()
        actualizarFiltro(_estado.value.filtro.copy(extensionesSeleccionadas = actuales - ext))
    }

    /** Detecta si el usuario escribió una extensión en la barra y la convierte en filtro automáticamente */
    fun detectarYAgregarExtensionDesdeQuery(texto: String): Boolean {
        val ext = FiltroBusqueda.detectarExtensionEnQuery(texto) ?: return false
        agregarExtension(ext)
        onQueryChange("")
        return true
    }

    /** Valida si una extensión es aceptable antes de agregarla (cualquier extensión alfanumérica 1-20) */
    fun esExtensionValida(extension: String): Boolean = FiltroBusqueda.esExtensionValida(extension)

    fun quitarTipo(tipo: TipoArchivo) {
        val actuales = _estado.value.filtro.tiposSeleccionados
        actualizarFiltro(_estado.value.filtro.copy(tiposSeleccionados = actuales - tipo))
    }

    fun setOrden(orden: OrdenBusqueda) {
        actualizarFiltro(_estado.value.filtro.copy(orden = orden))
    }

    fun toggleBuscarEnContenido() {
        actualizarFiltro(_estado.value.filtro.copy(buscarEnContenido = !_estado.value.filtro.buscarEnContenido))
    }

    fun toggleSeleccion(archivoId: Long) {
        _estado.update { actual ->
            val nuevos = if (archivoId in actual.seleccionados) {
                actual.seleccionados - archivoId
            } else {
                actual.seleccionados + archivoId
            }
            actual.copy(seleccionados = nuevos)
        }
    }

    fun seleccionarTodos() {
        _estado.update { actual ->
            val todosIds = actual.resultados.map { it.id }.toSet()
            actual.copy(seleccionados = todosIds)
        }
    }

    fun deseleccionarTodos() {
        _estado.update { it.copy(seleccionados = emptySet()) }
    }

    fun seleccionarVarios(ids: Set<Long>) {
        _estado.update { it.copy(seleccionados = it.seleccionados + ids) }
    }

    fun deseleccionarVarios(ids: Set<Long>) {
        _estado.update { it.copy(seleccionados = it.seleccionados - ids) }
    }

    fun eliminarSeleccionadosLocales(idsEliminados: Set<Long>) {
        _estado.update { actual ->
            val restantes = actual.resultados.filterNot { it.id in idsEliminados }
            val nuevosSeleccionados = actual.seleccionados - idsEliminados
            actual.copy(resultados = restantes, seleccionados = nuevosSeleccionados)
        }
    }

    fun refrescar() {
        viewModelScope.launch { ejecutarBusqueda(_estado.value.filtro) }
    }

    fun limpiarFiltros() {
        actualizarFiltro(FiltroBusqueda(query = _estado.value.filtro.query))
    }

    fun limpiarTodo() {
        val vacio = FiltroBusqueda()
        _estado.update { it.copy(filtro = vacio, resultados = emptyList(), seleccionados = emptySet(), cargando = false) }
        filtroFlow.value = vacio
    }

    fun cargarHistorial() {
        viewModelScope.launch { _historial.value = repositorio.obtenerHistorial() }
    }

    fun limpiarHistorial() {
        viewModelScope.launch {
            repositorio.limpiarHistorial()
            _historial.value = emptyList()
        }
    }

    private fun actualizarFiltro(nuevoFiltro: FiltroBusqueda) {
        _estado.update { it.copy(filtro = nuevoFiltro) }
        filtroFlow.value = nuevoFiltro
    }

    private suspend fun ejecutarBusqueda(filtro: FiltroBusqueda) {
        if (filtro.query.isBlank() && !filtro.tieneFiltrosActivos) {
            _estado.update { it.copy(resultados = emptyList(), cargando = false, tiempoBusquedaMs = 0) }
            return
        }
        _estado.update { it.copy(cargando = true, mensajeError = null) }
        val inicio = System.currentTimeMillis()
        try {
            buscarArchivos(filtro).collect { lista ->
                val tiempo = System.currentTimeMillis() - inicio
                _estado.update {
                    it.copy(
                        resultados = lista,
                        cargando = false,
                        tiempoBusquedaMs = tiempo
                    )
                }
            }
            // Guarda la búsqueda en el historial si tiene texto
            if (filtro.query.isNotBlank()) {
                repositorio.guardarHistorial(filtro.query)
                _historial.value = repositorio.obtenerHistorial()
            }
        } catch (e: Exception) {
            _estado.update { it.copy(cargando = false, mensajeError = e.message) }
        }
    }
}
