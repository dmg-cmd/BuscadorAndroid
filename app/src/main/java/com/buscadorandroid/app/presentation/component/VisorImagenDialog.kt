package com.buscadorandroid.app.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.buscadorandroid.app.domain.model.Archivo
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VisorImagenDialog(
    listaArchivos: List<Archivo>,
    indiceInicial: Int,
    seleccionados: Set<Long>,
    onToggleSeleccion: (Archivo) -> Unit,
    onDismiss: () -> Unit,
    onCompartir: (Archivo) -> Unit,
    onAbrirExterno: (Archivo) -> Unit
) {
    if (listaArchivos.isEmpty()) return

    val total = listaArchivos.size
    val paginaInicial = indiceInicial.coerceIn(0, total - 1)
    val pagerState = rememberPagerState(initialPage = paginaInicial) { total }
    val coroutineScope = rememberCoroutineScope()
    val archivoActual = listaArchivos.getOrNull(pagerState.currentPage) ?: return
    val estaMarcado = archivoActual.id in seleccionados

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
            // Paginador horizontal para deslizar entre imágenes
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                val archivo = listaArchivos[page]
                ItemImagenZoom(
                    archivo = archivo,
                    esPaginaActiva = page == pagerState.currentPage
                )
            }

            // Flecha lateral Izquierda (Anterior)
            if (pagerState.currentPage > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Foto anterior",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp).padding(start = 4.dp)
                        )
                    }
                }
            }

            // Flecha lateral Derecha (Siguiente)
            if (pagerState.currentPage < total - 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Foto siguiente",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Barra superior
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / $total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ${archivoActual.nombre}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${archivoActual.tamanoLegible} • .${archivoActual.extension}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    // Botón para marcar / desmarcar para eliminación
                    FilledTonalButton(
                        onClick = { onToggleSeleccion(archivoActual) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (estaMarcado) MaterialTheme.colorScheme.primary else Color.DarkGray,
                            contentColor = if (estaMarcado) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (estaMarcado) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (estaMarcado) "Marcada" else "Marcar",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    IconButton(onClick = { onCompartir(archivoActual) }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White)
                    }
                    IconButton(onClick = { onAbrirExterno(archivoActual) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir con otra app", tint = Color.White)
                    }
                }
            }

            // Barra inferior con información de ruta y navegación
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Desliza a los lados para cambiar de foto • Pellizca para zoom",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = archivoActual.ruta.ifEmpty { archivoActual.uri.toString() },
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

@Composable
private fun ItemImagenZoom(
    archivo: Archivo,
    esPaginaActiva: Boolean
) {
    val contexto = LocalContext.current
    var escala by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(esPaginaActiva) {
        if (!esPaginaActiva) {
            escala = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(archivo.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (escala > 1f) {
                            escala = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            escala = 2.5f
                        }
                    }
                )
            }
            .pointerInput(archivo.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nuevaEscala = (escala * zoom).coerceIn(1f, 5f)
                    escala = nuevaEscala
                    if (nuevaEscala > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(contexto)
                .data(archivo.uri)
                .crossfade(true)
                .build(),
            contentDescription = archivo.nombre,
            contentScale = ContentScale.Fit,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se pudo cargar la imagen", color = Color.White)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = escala,
                    scaleY = escala,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}
