package com.buscadorandroid.app.presentation.component

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.buscadorandroid.app.domain.model.Archivo

@OptIn(UnstableApi::class)
@Composable
fun ReproductorVideoDialog(
    archivo: Archivo?,
    estaSeleccionado: Boolean = false,
    onToggleSeleccion: (Archivo) -> Unit = {},
    onDismiss: () -> Unit,
    onCompartir: (Archivo) -> Unit,
    onAbrirExterno: (Archivo) -> Unit
) {
    if (archivo == null) return

    val contexto = LocalContext.current

    val player = remember(archivo.id) {
        ExoPlayer.Builder(contexto).build().apply {
            setMediaItem(MediaItem.fromUri(archivo.uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Reproductor ExoPlayer
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Barra superior
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = archivo.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${archivo.tamanoLegible} • .${archivo.extension}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    // Botón para marcar video
                    IconButton(onClick = { onToggleSeleccion(archivo) }) {
                        Icon(
                            imageVector = if (estaSeleccionado) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                            contentDescription = if (estaSeleccionado) "Marcado para eliminar" else "Marcar video",
                            tint = if (estaSeleccionado) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    IconButton(onClick = { onCompartir(archivo) }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White)
                    }
                    IconButton(onClick = { onAbrirExterno(archivo) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir con otra app", tint = Color.White)
                    }
                }
            }

            // Barra inferior con información del video
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = archivo.ruta.ifEmpty { archivo.uri.toString() },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
