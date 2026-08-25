package com.buscadorandroid.app.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.buscadorandroid.app.domain.model.Archivo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ReproductorAudioDialog(
    archivo: Archivo?,
    estaSeleccionado: Boolean = false,
    onToggleSeleccion: (Archivo) -> Unit = {},
    onDismiss: () -> Unit,
    onCompartir: (Archivo) -> Unit,
    onAbrirExterno: (Archivo) -> Unit
) {
    if (archivo == null) return

    val contexto = LocalContext.current
    var estaReproduciendo by remember { mutableStateOf(true) }
    var posicionActualMs by remember { mutableLongStateOf(0L) }
    var duracionMs by remember { mutableLongStateOf(0L) }
    var arrastrandoSlider by remember { mutableStateOf(false) }
    var posicionSlider by remember { mutableFloatStateOf(0f) }

    val player = remember(archivo.id) {
        ExoPlayer.Builder(contexto).build().apply {
            setMediaItem(MediaItem.fromUri(archivo.uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                estaReproduciendo = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val d = player.duration
                    if (d > 0) duracionMs = d
                } else if (playbackState == Player.STATE_ENDED) {
                    estaReproduciendo = false
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    // Actualizar posición periódicamente
    LaunchedEffect(player, estaReproduciendo) {
        while (isActive) {
            if (!arrastrandoSlider) {
                posicionActualMs = player.currentPosition
                val d = player.duration
                if (d > 0) duracionMs = d
            }
            delay(300)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera con botón cerrar y opciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reproduciendo audio",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón marcar audio
                        IconButton(onClick = { onToggleSeleccion(archivo) }) {
                            Icon(
                                imageVector = if (estaSeleccionado) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = if (estaSeleccionado) "Marcado para eliminar" else "Marcar audio",
                                tint = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onCompartir(archivo) }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onAbrirExterno(archivo) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir con otra app", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Icono central / carátula de música
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nombre y detalles
                Text(
                    text = archivo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${archivo.tamanoLegible} • .${archivo.extension}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de progreso (Slider)
                val valorSlider = if (arrastrandoSlider) {
                    posicionSlider
                } else {
                    if (duracionMs > 0) (posicionActualMs.toFloat() / duracionMs).coerceIn(0f, 1f) else 0f
                }

                Slider(
                    value = valorSlider,
                    onValueChange = {
                        arrastrandoSlider = true
                        posicionSlider = it
                    },
                    onValueChangeFinished = {
                        val destinoMs = (posicionSlider * duracionMs).toLong()
                        player.seekTo(destinoMs)
                        posicionActualMs = destinoMs
                        arrastrandoSlider = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Tiempos (actual / total)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val actualTexto = if (arrastrandoSlider) {
                        formatearTiempoMs((posicionSlider * duracionMs).toLong())
                    } else {
                        formatearTiempoMs(posicionActualMs)
                    }
                    Text(text = actualTexto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatearTiempoMs(duracionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controles de Reproducción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retroceder 10s
                    IconButton(onClick = {
                        val nuevaPos = maxOf(0L, player.currentPosition - 10000L)
                        player.seekTo(nuevaPos)
                        posicionActualMs = nuevaPos
                    }) {
                        Icon(Icons.Default.Replay10, contentDescription = "Retroceder 10 segundos", modifier = Modifier.size(32.dp))
                    }

                    // Botón Play / Pause
                    FilledIconButton(
                        onClick = {
                            if (estaReproduciendo) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_ENDED) {
                                    player.seekTo(0)
                                }
                                player.play()
                            }
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (estaReproduciendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (estaReproduciendo) "Pausar" else "Reproducir",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Adelantar 10s
                    IconButton(onClick = {
                        val nuevaPos = if (duracionMs > 0) minOf(duracionMs, player.currentPosition + 10000L) else player.currentPosition + 10000L
                        player.seekTo(nuevaPos)
                        posicionActualMs = nuevaPos
                    }) {
                        Icon(Icons.Default.Forward10, contentDescription = "Adelantar 10 segundos", modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

private fun formatearTiempoMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSegundos = ms / 1000
    val minutos = totalSegundos / 60
    val segundos = totalSegundos % 60
    return String.format("%02d:%02d", minutos, segundos)
}
