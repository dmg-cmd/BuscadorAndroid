package com.buscadorandroid.app.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.TipoArchivo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemArchivo(
    archivo: Archivo,
    estaSeleccionado: Boolean = false,
    hayModoSeleccion: Boolean = false,
    onToggleSeleccion: (Archivo) -> Unit = {},
    onAbrir: (Archivo) -> Unit,
    onCompartir: (Archivo) -> Unit,
    onCopiarRuta: (Archivo) -> Unit,
    onVerImagen: (Archivo) -> Unit = onAbrir,
    onReproducirVideo: (Archivo) -> Unit = onAbrir,
    onReproducirAudio: (Archivo) -> Unit = onAbrir,
    onEliminarIndividual: (Archivo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var mostrarMenu by remember { mutableStateOf(false) }
    val esImagen = archivo.tipo == TipoArchivo.IMAGEN
    val esVideo = archivo.tipo == TipoArchivo.VIDEO
    val esAudio = archivo.tipo == TipoArchivo.AUDIO
    val contexto = LocalContext.current

    val accionClickPrincipal: () -> Unit = {
        when {
            esImagen -> onVerImagen(archivo)
            esVideo -> onReproducirVideo(archivo)
            esAudio -> onReproducirAudio(archivo)
            else -> onAbrir(archivo)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .combinedClickable(
                onClick = accionClickPrincipal,
                onLongClick = { onToggleSeleccion(archivo) }
            )
            .then(
                if (estaSeleccionado) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CardDefaults.shape
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (estaSeleccionado) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (estaSeleccionado) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox visible siempre en cada fila directamente
            Checkbox(
                checked = estaSeleccionado,
                onCheckedChange = { onToggleSeleccion(archivo) },
                modifier = Modifier.padding(end = 4.dp)
            )

            // Miniatura visual o Icono
            if (esImagen || esVideo) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(contexto)
                            .data(archivo.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = archivo.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Icon(
                                imageVector = archivo.tipo.icono(),
                                contentDescription = archivo.tipo.etiqueta,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (esAudio) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = archivo.tipo.icono(),
                        contentDescription = archivo.tipo.etiqueta,
                        modifier = Modifier.size(26.dp),
                        tint = if (esAudio) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Información del archivo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = archivo.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = archivo.ruta.ifEmpty { archivo.uri.toString() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = archivo.tamanoLegible,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "• .${archivo.extension}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "• ${formatearFecha(archivo.fechaModificacion)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Opciones del menú
            Box {
                IconButton(onClick = { mostrarMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (estaSeleccionado) "Desmarcar" else "Marcar para eliminar") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        onClick = {
                            mostrarMenu = false
                            onToggleSeleccion(archivo)
                        }
                    )
                    if (esImagen) {
                        DropdownMenuItem(
                            text = { Text("Ver imagen") },
                            onClick = {
                                mostrarMenu = false
                                onVerImagen(archivo)
                            }
                        )
                    } else if (esVideo) {
                        DropdownMenuItem(
                            text = { Text("Reproducir video") },
                            onClick = {
                                mostrarMenu = false
                                onReproducirVideo(archivo)
                            }
                        )
                    } else if (esAudio) {
                        DropdownMenuItem(
                            text = { Text("Reproducir audio") },
                            onClick = {
                                mostrarMenu = false
                                onReproducirAudio(archivo)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Abrir con app externa") },
                        onClick = {
                            mostrarMenu = false
                            onAbrir(archivo)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compartir") },
                        onClick = {
                            mostrarMenu = false
                            onCompartir(archivo)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copiar ruta") },
                        onClick = {
                            mostrarMenu = false
                            onCopiarRuta(archivo)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Eliminar / Enviar a papelera", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            mostrarMenu = false
                            onEliminarIndividual(archivo)
                        }
                    )
                }
            }
        }
    }
}

private fun formatearFecha(ms: Long): String {
    if (ms == 0L) return ""
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(ms))
}
