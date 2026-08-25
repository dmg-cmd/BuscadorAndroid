package com.buscadorandroid.app.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.buscadorandroid.app.presentation.theme.BuscadorTheme
import com.buscadorandroid.app.presentation.theme.TemaModo
import com.buscadorandroid.app.presentation.theme.TemaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val temaVm = hiltViewModel<TemaViewModel>()
            val modo by temaVm.modo.collectAsState()
            val temaOscuro = when (modo) {
                TemaModo.SISTEMA -> isSystemInDarkTheme()
                TemaModo.CLARO -> false
                TemaModo.OSCURO -> true
            }
            BuscadorTheme(darkTheme = temaOscuro) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaBusqueda()
                }
            }
        }
    }
}
