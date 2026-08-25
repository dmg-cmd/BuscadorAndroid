package com.buscadorandroid.app.presentation.theme

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mantiene el modo de tema elegido por el usuario (Sistema / Claro / Oscuro)
 * y lo persiste en SharedPreferences para que sobreviva a reinicios.
 */
@HiltViewModel
class TemaViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences(NOMBRE_PREF, Application.MODE_PRIVATE)

    private val _modo = MutableStateFlow(leerModo())
    val modo = _modo.asStateFlow()

    private fun leerModo(): TemaModo {
        return when (prefs.getString(CLAVE, TemaModo.SISTEMA.name)) {
            TemaModo.CLARO.name -> TemaModo.CLARO
            TemaModo.OSCURO.name -> TemaModo.OSCURO
            else -> TemaModo.SISTEMA
        }
    }

    /** Cicla Sistema -> Claro -> Oscuro -> Sistema. */
    fun ciclarModo() {
        val siguiente = when (_modo.value) {
            TemaModo.SISTEMA -> TemaModo.CLARO
            TemaModo.CLARO -> TemaModo.OSCURO
            TemaModo.OSCURO -> TemaModo.SISTEMA
        }
        guardar(siguiente)
    }

    fun establecerModo(modo: TemaModo) = guardar(modo)

    private fun guardar(modo: TemaModo) {
        viewModelScope.launch {
            prefs.edit().putString(CLAVE, modo.name).apply()
            _modo.value = modo
        }
    }

    companion object {
        private const val NOMBRE_PREF = "tema_prefs"
        private const val CLAVE = "modo_tema"
    }
}
