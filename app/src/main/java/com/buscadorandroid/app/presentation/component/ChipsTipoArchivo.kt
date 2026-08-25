package com.buscadorandroid.app.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.buscadorandroid.app.domain.model.TipoArchivo

/**
 * Mapea cada TipoArchivo a su icono
 */
fun TipoArchivo.icono(): ImageVector = when (this) {
    TipoArchivo.IMAGEN -> Icons.Default.Image
    TipoArchivo.VIDEO -> Icons.Default.VideoLibrary
    TipoArchivo.AUDIO -> Icons.Default.MusicNote
    TipoArchivo.DOCUMENTO -> Icons.Default.Description
    TipoArchivo.APK -> Icons.Default.Android
    TipoArchivo.COMPRIMIDO -> Icons.Default.FolderZip
    TipoArchivo.OTRO -> Icons.Default.InsertDriveFile
}

@Composable
fun FilaChipsTipo(
    tiposSeleccionados: Set<TipoArchivo>,
    onToggleTipo: (TipoArchivo) -> Unit,
    extensionesSeleccionadas: Set<String>,
    onQuitarExtension: (String) -> Unit,
    onAgregarExtensionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(TipoArchivo.entries.filter { it != TipoArchivo.OTRO }) { tipo ->
                val seleccionado = tipo in tiposSeleccionados
                FilterChip(
                    selected = seleccionado,
                    onClick = { onToggleTipo(tipo) },
                    label = { Text(tipo.etiqueta) },
                    leadingIcon = {
                        Icon(
                            imageVector = tipo.icono(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            item {
                AssistChip(
                    onClick = onAgregarExtensionClick,
                    label = { Text("+ Extensión") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // Mostrar extensiones manuales seleccionadas - CUALQUIER extensión (log, tmp, dwg, bak, etc.)
        if (extensionesSeleccionadas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(extensionesSeleccionadas.sorted()) { ext ->
                    InputChip(
                        selected = true,
                        onClick = { onQuitarExtension(ext) },
                        label = { Text(".$ext") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Quitar .$ext", modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Resumen de filtros activos
        if (tiposSeleccionados.isNotEmpty() || extensionesSeleccionadas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            val partes = mutableListOf<String>()
            if (tiposSeleccionados.isNotEmpty()) partes.add(tiposSeleccionados.joinToString { it.etiqueta })
            if (extensionesSeleccionadas.isNotEmpty()) partes.add(extensionesSeleccionadas.sorted().joinToString(", ") { ".$it" })
            Text(
                text = "Filtrando por: ${partes.joinToString(" + ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoAgregarExtension(
    mostrar: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    if (!mostrar) return

    var texto by remember { mutableStateOf("") }
    var mostrarError by remember { mutableStateOf(false) }

    // Validación en tiempo real: cualquier extensión alfanumérica 1-20 es válida
    val esValida = remember(texto) {
        val limpia = texto.lowercase().trim().trimStart('.').trim()
            .removePrefix("*.").removePrefix("ext:")
            .trimStart('.').trim()
        limpia.isEmpty() || limpia.matches(Regex("^[a-z0-9]{1,20}$"))
    }
    val limpiaPreview = remember(texto) {
        texto.lowercase().trim().removePrefix("*.").removePrefix("ext:").trimStart('.').trim()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar extensión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Escribe CUALQUIER extensión, con o sin punto. Ejemplos: pdf, log, tmp, dwg, bak, db, xyz, 123",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = {
                        texto = it
                        if (mostrarError) mostrarError = false
                    },
                    placeholder = { Text("ej: log") },
                    singleLine = true,
                    prefix = { Text(".") },
                    isError = !esValida,
                    supportingText = {
                        when {
                            !esValida -> Text("Solo letras y números, 1-20 caracteres", color = MaterialTheme.colorScheme.error)
                            limpiaPreview.isNotEmpty() -> Text("Se guardará como .$limpiaPreview", color = MaterialTheme.colorScheme.primary)
                            else -> Text("Puedes buscar extensiones raras: .tmp, .bak, .dwg, .log, etc.")
                        }
                    }
                )
                Text("Sugerencias (toca para usar):", style = MaterialTheme.typography.labelSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("pdf", "log", "tmp", "bak", "dwg", "db", "json", "xml", "mp3", "jpg", "zip", "txt").forEach { sug ->
                        SuggestionChip(
                            onClick = { texto = sug },
                            label = { Text(sug) }
                        )
                    }
                }
                Text(
                    "Tip: también puedes escribir \".log\" o \"*.tmp\" directamente en la barra de búsqueda",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limpia = texto.lowercase().trim().removePrefix("*.").removePrefix("ext:").trimStart('.').trim()
                    if (limpia.isEmpty() || !limpia.matches(Regex("^[a-z0-9]{1,20}$"))) {
                        mostrarError = true
                        return@TextButton
                    }
                    onConfirmar(limpia)
                    onDismiss()
                },
                enabled = esValida && limpiaPreview.isNotEmpty()
            ) { Text("Agregar .$limpiaPreview") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
