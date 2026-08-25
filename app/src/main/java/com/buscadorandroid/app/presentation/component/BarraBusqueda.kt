package com.buscadorandroid.app.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.buscadorandroid.app.domain.model.FiltroBusqueda

@Composable
fun BarraBusqueda(
    query: String,
    onQueryChange: (String) -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier,
    onExtensionDetectada: ((String) -> Unit)? = null
) {
    // Detecta si el usuario está escribiendo una extensión suelta para sugerir
    val extensionDetectada = FiltroBusqueda.detectarExtensionEnQuery(query)

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Buscar archivos...  (ej: vacaciones, .log, *.tmp)") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onLimpiar) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        supportingText = {
            if (extensionDetectada != null && onExtensionDetectada != null) {
                Text("¿Quieres filtrar por .$extensionDetectada? Toca aquí →", color = MaterialTheme.colorScheme.primary)
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}
