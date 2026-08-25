package com.buscadorandroid.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Esquemas de respaldo (usados si el dispositivo no soporta color dinámico)
private val EsquemaClaro = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0B57D0),
    secondary = androidx.compose.ui.graphics.Color(0xFF535F70),
    tertiary = androidx.compose.ui.graphics.Color(0xFF6B5778)
)

private val EsquemaOscuro = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF9FC2FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFBFC8D7),
    tertiary = androidx.compose.ui.graphics.Color(0xFFD7C2E0)
)

/**
 * Tema de la app con soporte de modo oscuro y color dinámico (Material You).
 * En Android 12+ usa los colores del sistema si dynamicColor está activo.
 */
@Composable
fun BuscadorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val contexto = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        }
        darkTheme -> EsquemaOscuro
        else -> EsquemaClaro
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
