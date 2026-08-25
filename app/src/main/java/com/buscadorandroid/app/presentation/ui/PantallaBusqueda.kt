package com.buscadorandroid.app.presentation.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.FiltroBusqueda
import com.buscadorandroid.app.domain.model.OrdenBusqueda
import com.buscadorandroid.app.presentation.component.*
import com.buscadorandroid.app.presentation.viewmodel.BusquedaViewModel
import com.buscadorandroid.app.presentation.theme.TemaModo
import com.buscadorandroid.app.presentation.theme.TemaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaBusqueda(
    viewModel: BusquedaViewModel = hiltViewModel(),
    temaVm: TemaViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val modo by temaVm.modo.collectAsState()
    val contexto = LocalContext.current
    var mostrarDialogoExt by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarDialogoEnviar by remember { mutableStateOf(false) }
    var archivoAEliminarIndividual by remember { mutableStateOf<Archivo?>(null) }
    var agruparPorCarpeta by remember { mutableStateOf(true) }
    var carpetaSeleccionada by remember { mutableStateOf<String?>(null) }

    var imagenSeleccionada by remember { mutableStateOf<Archivo?>(null) }
    var indiceImagenSeleccionada by remember { mutableIntStateOf(-1) }
    var videoSeleccionado by remember { mutableStateOf<Archivo?>(null) }
    var audioSeleccionado by remember { mutableStateOf<Archivo?>(null) }

    var mostrarMenuOrden by remember { mutableStateOf(false) }
    val historial by viewModel.historial.collectAsState()

    // Interceptar botón atrás si estamos dentro de una carpeta
    BackHandler(enabled = carpetaSeleccionada != null) {
        carpetaSeleccionada = null
    }

    // Launcher para confirmación nativa de MediaStore (Trash / Delete en Android 11+)
    val launcherIntentSender = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(contexto, "Operación realizada con éxito", Toast.LENGTH_SHORT).show()
            viewModel.refrescar()
            viewModel.deseleccionarTodos()
        }
    }

    val archivosAProcesar = remember(estado.seleccionados, estado.resultados, archivoAEliminarIndividual) {
        if (archivoAEliminarIndividual != null) {
            listOf(archivoAEliminarIndividual!!)
        } else {
            estado.resultados.filter { it.id in estado.seleccionados }
        }
    }

    // Launcher para seleccionar carpeta de destino para COPIAR
    val launcherSeleccionarCarpetaCopiar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            transferirArchivosACarpeta(
                contexto = contexto,
                archivos = archivosAProcesar,
                treeUri = treeUri,
                esMover = false,
                onFin = {
                    viewModel.deseleccionarTodos()
                }
            )
        }
    }

    // Launcher para seleccionar carpeta de destino para MOVER
    val launcherSeleccionarCarpetaMover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            transferirArchivosACarpeta(
                contexto = contexto,
                archivos = archivosAProcesar,
                treeUri = treeUri,
                esMover = true,
                onFin = {
                    // Tras mover, enviamos originales a papelera o refrescamos
                    ejecutarPapelera(
                        contexto = contexto,
                        archivos = archivosAProcesar,
                        launcherIntentSender = launcherIntentSender,
                        onExitoDirecto = { lista ->
                            viewModel.eliminarSeleccionadosLocales(lista.map { it.id }.toSet())
                        }
                    )
                }
            )
        }
    }

    // Permisos según versión de Android
    val permisos = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val permisosState = rememberMultiplePermissionsState(permisos)

    val launcherAjustes = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    // Agrupación de resultados por carpeta
    val gruposPorCarpeta = remember(estado.resultados) {
        estado.resultados.groupBy { it.nombreCarpetaSimple }
    }

    // Lista de imágenes disponibles para el visor en pantalla completa
    val listaImagenesActuales = remember(estado.resultados, carpetaSeleccionada, agruparPorCarpeta) {
        val base = if (agruparPorCarpeta && carpetaSeleccionada != null) {
            gruposPorCarpeta[carpetaSeleccionada] ?: emptyList()
        } else {
            estado.resultados
        }
        base.filter { it.tipo == com.buscadorandroid.app.domain.model.TipoArchivo.IMAGEN }
    }

    // Si la carpeta seleccionada ya no existe tras una búsqueda/filtro, resetear a null
    LaunchedEffect(gruposPorCarpeta, carpetaSeleccionada) {
        if (carpetaSeleccionada != null && !gruposPorCarpeta.containsKey(carpetaSeleccionada)) {
            carpetaSeleccionada = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(com.buscadorandroid.app.R.drawable.logo_buscador),
                            contentDescription = "Logo BuscadorAndroid",
                            modifier = Modifier.size(30.dp)
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "v${com.buscadorandroid.app.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { temaVm.ciclarModo() }) {
                        val iconoTema = when (modo) {
                            TemaModo.SISTEMA -> Icons.Filled.BrightnessAuto
                            TemaModo.CLARO -> Icons.Filled.LightMode
                            TemaModo.OSCURO -> Icons.Filled.DarkMode
                        }
                        Icon(
                            imageVector = iconoTema,
                            contentDescription = "Cambiar tema (actual: $modo)"
                        )
                    }
                    if (estado.filtro.tieneFiltrosActivos && !estado.haySeleccion) {
                        TextButton(onClick = { viewModel.limpiarFiltros() }) {
                            Text("Limpiar filtros")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BarraBusqueda(
                    query = estado.filtro.query,
                    onQueryChange = {
                        viewModel.onQueryChange(it)
                        carpetaSeleccionada = null
                    },
                    onLimpiar = {
                        viewModel.limpiarTodo()
                        carpetaSeleccionada = null
                    },
                    onExtensionDetectada = { ext -> viewModel.agregarExtension(ext) }
                )

                // Sugerencia clickeable si se detecta extensión en la query
                val extDetectada = FiltroBusqueda.detectarExtensionEnQuery(estado.filtro.query)
                if (extDetectada != null) {
                    SuggestionChip(
                        onClick = {
                            viewModel.agregarExtension(extDetectada)
                            viewModel.onQueryChange("")
                        },
                        label = { Text("Filtrar solo .$extDetectada") },
                        icon = { Icon(Icons.Default.FilterList, contentDescription = null) }
                    )
                }

                // Fila de filtros por tipo + extensión
                FilaChipsTipo(
                    tiposSeleccionados = estado.filtro.tiposSeleccionados,
                    onToggleTipo = {
                        viewModel.toggleTipo(it)
                        carpetaSeleccionada = null
                    },
                    extensionesSeleccionadas = estado.filtro.extensionesSeleccionadas,
                    onQuitarExtension = {
                        viewModel.quitarExtension(it)
                        carpetaSeleccionada = null
                    },
                    onAgregarExtensionClick = { mostrarDialogoExt = true }
                )

                DialogoAgregarExtension(
                    mostrar = mostrarDialogoExt,
                    onDismiss = { mostrarDialogoExt = false },
                    onConfirmar = {
                        viewModel.agregarExtension(it)
                        carpetaSeleccionada = null
                    }
                )

                // Controles unificados en una sola fila (mismo estilo, icono diferenciador)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // En contenido (alterna)
                    OutlinedButton(
                        onClick = { viewModel.toggleBuscarEnContenido() },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (estado.filtro.buscarEnContenido) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.TextSnippet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("En contenido", style = MaterialTheme.typography.labelSmall)
                    }

                    // Orden (abre menú)
                    Box {
                        OutlinedButton(
                            onClick = { mostrarMenuOrden = true },
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Orden", style = MaterialTheme.typography.labelSmall)
                        }
                        DropdownMenu(
                            expanded = mostrarMenuOrden,
                            onDismissRequest = { mostrarMenuOrden = false }
                        ) {
                            OrdenBusqueda.entries.forEach { orden ->
                                DropdownMenuItem(
                                    text = { Text(orden.etiqueta) },
                                    onClick = {
                                        viewModel.setOrden(orden)
                                        mostrarMenuOrden = false
                                    },
                                    trailingIcon = if (estado.filtro.orden == orden) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Por carpeta (alterna agrupación)
                    OutlinedButton(
                        onClick = {
                            agruparPorCarpeta = !agruparPorCarpeta
                            carpetaSeleccionada = null
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (agruparPorCarpeta) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (agruparPorCarpeta) Icons.Default.Folder else Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Por carpeta", style = MaterialTheme.typography.labelSmall)
                    }

                    // Seleccionar todo / Deseleccionar todo
                    OutlinedButton(
                        onClick = {
                            if (estado.todosSeleccionados) viewModel.deseleccionarTodos() else viewModel.seleccionarTodos()
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (estado.todosSeleccionados) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (estado.todosSeleccionados) Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (estado.todosSeleccionados) "Deseleccionar todo" else "Seleccionar todo",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Aviso de permisos
                if (!permisosState.allPermissionsGranted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Se necesitan permisos para buscar archivos", style = MaterialTheme.typography.titleSmall)
                            Text("Sin permisos solo verás resultados limitados.", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { permisosState.launchMultiplePermissionRequest() }) {
                                Text("Conceder permisos")
                            }
                        }
                    }
                } else {
                    // Botón búsqueda profunda opcional
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Búsqueda profunda", style = MaterialTheme.typography.titleSmall)
                                    Text("Busca también en WhatsApp, carpetas ocultas, etc.", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = android.net.Uri.parse("package:${contexto.packageName}")
                                    }
                                    launcherAjustes.launch(intent)
                                }) {
                                    Text("Activar")
                                }
                            }
                        }
                    }
                }

                // Estado de carga / contador / lista
                when {
                    estado.cargando -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    estado.filtro.query.isBlank() && !estado.filtro.tieneFiltrosActivos -> {
                        if (historial.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Busca por nombre y filtra por tipo", style = MaterialTheme.typography.bodyMedium)
                                    Text("Ej: escribe \"vacaciones\" y toca Imágenes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Búsquedas recientes", style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = { viewModel.limpiarHistorial() }) {
                                        Text("Limpiar")
                                    }
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    historial.forEach { texto ->
                                        SuggestionChip(
                                            onClick = { viewModel.onQueryChange(texto) },
                                            label = { Text(texto) },
                                            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    estado.resultados.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            val detalleFiltro = buildString {
                                if (estado.filtro.query.isNotBlank()) append("\"${estado.filtro.query}\" ")
                                if (estado.filtro.extensionesEfectivas.isNotEmpty()) append("con extensión ${estado.filtro.extensionesEfectivas.joinToString { ".$it" }} ")
                                if (estado.filtro.tiposSeleccionados.isNotEmpty()) append("tipo ${estado.filtro.tiposSeleccionados.joinToString { it.etiqueta }}")
                            }.ifBlank { "con los filtros actuales" }
                            Text("No se encontraron archivos $detalleFiltro", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    else -> {
                        // Contador de resultados (los controles están en la fila superior unificada)
                        Text(
                            text = if (agruparPorCarpeta && carpetaSeleccionada == null) {
                                "${gruposPorCarpeta.size} carpetas (${estado.resultados.size} archivos)"
                            } else {
                                "${estado.resultados.size} archivos (${estado.tiempoBusquedaMs}ms)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // Vista cuando se está dentro de una carpeta seleccionada
                        if (agruparPorCarpeta && carpetaSeleccionada != null) {
                            val archivosDeEstaCarpeta = gruposPorCarpeta[carpetaSeleccionada] ?: emptyList()
                            val todosDeEstaCarpetaMarcados = archivosDeEstaCarpeta.isNotEmpty() && archivosDeEstaCarpeta.all { it.id in estado.seleccionados }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(onClick = { carpetaSeleccionada = null }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Volver a carpetas"
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = carpetaSeleccionada!!,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${archivosDeEstaCarpeta.size} archivos en esta carpeta",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            val idsCarpeta = archivosDeEstaCarpeta.map { it.id }.toSet()
                                            if (todosDeEstaCarpetaMarcados) {
                                                viewModel.deseleccionarVarios(idsCarpeta)
                                            } else {
                                                viewModel.seleccionarVarios(idsCarpeta)
                                            }
                                        }
                                    ) {
                                        Text(if (todosDeEstaCarpetaMarcados) "Deseleccionar carpeta" else "Marcar carpeta")
                                    }
                                }
                            }

                            // Lista de archivos dentro de la carpeta
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = if (estado.haySeleccion) 80.dp else 16.dp)
                            ) {
                                items(archivosDeEstaCarpeta, key = { it.id }) { archivo ->
                                    val estaMarcado = archivo.id in estado.seleccionados
                                    ItemArchivo(
                                        archivo = archivo,
                                        estaSeleccionado = estaMarcado,
                                        hayModoSeleccion = estado.haySeleccion,
                                        onToggleSeleccion = { viewModel.toggleSeleccion(it.id) },
                                        onAbrir = { abrirArchivo(contexto, it) },
                                        onCompartir = { compartirArchivo(contexto, it) },
                                        onCopiarRuta = { copiarRuta(contexto, it) },
                                        onVerImagen = { foto ->
                                            val idx = listaImagenesActuales.indexOfFirst { it.id == foto.id }
                                            indiceImagenSeleccionada = if (idx != -1) idx else 0
                                            imagenSeleccionada = foto
                                        },
                                        onReproducirVideo = { videoSeleccionado = it },
                                        onReproducirAudio = { audioSeleccionado = it },
                                        onEliminarIndividual = {
                                            archivoAEliminarIndividual = it
                                            mostrarDialogoEliminar = true
                                        }
                                    )
                                }
                            }
                        } else if (agruparPorCarpeta && carpetaSeleccionada == null) {
                            // Vista de Nivel 1: Lista de carpetas con cantidad de archivos
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = if (estado.haySeleccion) 80.dp else 16.dp)
                            ) {
                                items(gruposPorCarpeta.keys.toList(), key = { it }) { nombreCarpeta ->
                                    val archivos = gruposPorCarpeta[nombreCarpeta] ?: emptyList()
                                    val todosMarcados = archivos.isNotEmpty() && archivos.all { it.id in estado.seleccionados }
                                    val cantidadMarcados = archivos.count { it.id in estado.seleccionados }
                                    val totalBytes = archivos.sumOf { it.tamanoBytes }

                                    Card(
                                        onClick = { carpetaSeleccionada = nombreCarpeta },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (todosMarcados) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (todosMarcados) {
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CardDefaults.shape)
                                                } else Modifier
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = todosMarcados,
                                                onCheckedChange = {
                                                    val ids = archivos.map { it.id }.toSet()
                                                    if (todosMarcados) viewModel.deseleccionarVarios(ids) else viewModel.seleccionarVarios(ids)
                                                },
                                                modifier = Modifier.padding(end = 4.dp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = nombreCarpeta,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${archivos.size} ${if (archivos.size == 1) "archivo" else "archivos"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "• ${Archivo.formatearTamano(totalBytes)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (cantidadMarcados > 0 && !todosMarcados) {
                                                        Text(
                                                            text = "($cantidadMarcados marcados)",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                                contentDescription = "Entrar en carpeta",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Vista de lista plana normal
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = if (estado.haySeleccion) 80.dp else 16.dp)
                            ) {
                                items(estado.resultados, key = { it.id }) { archivo ->
                                    val estaMarcado = archivo.id in estado.seleccionados
                                    ItemArchivo(
                                        archivo = archivo,
                                        estaSeleccionado = estaMarcado,
                                        hayModoSeleccion = estado.haySeleccion,
                                        onToggleSeleccion = { viewModel.toggleSeleccion(it.id) },
                                        onAbrir = { abrirArchivo(contexto, it) },
                                        onCompartir = { compartirArchivo(contexto, it) },
                                        onCopiarRuta = { copiarRuta(contexto, it) },
                                        onVerImagen = { foto ->
                                            val idx = listaImagenesActuales.indexOfFirst { it.id == foto.id }
                                            indiceImagenSeleccionada = if (idx != -1) idx else 0
                                            imagenSeleccionada = foto
                                        },
                                        onReproducirVideo = { videoSeleccionado = it },
                                        onReproducirAudio = { audioSeleccionado = it },
                                        onEliminarIndividual = {
                                            archivoAEliminarIndividual = it
                                            mostrarDialogoEliminar = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Barra flotante de acciones para elementos seleccionados
            AnimatedVisibility(
                visible = estado.haySeleccion,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { viewModel.deseleccionarTodos() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                            }
                            Text(
                                text = "${estado.cantidadSeleccionados} marcados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Botón Enviar a carpeta o Nube
                            FilledTonalButton(
                                onClick = { mostrarDialogoEnviar = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enviar a...", style = MaterialTheme.typography.labelMedium)
                            }

                            // Botón compartir selección general
                            IconButton(onClick = {
                                val seleccionados = estado.resultados.filter { it.id in estado.seleccionados }
                                compartirMultiplesArchivos(contexto, seleccionados)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir seleccionados")
                            }

                            // Botón eliminar / papelera
                            IconButton(
                                onClick = {
                                    archivoAEliminarIndividual = null
                                    mostrarDialogoEliminar = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diálogo para enviar archivos a la Nube o Copiar/Mover a carpeta local
        DialogoEnviarArchivos(
            mostrar = mostrarDialogoEnviar,
            cantidadArchivos = archivosAProcesar.size,
            onDismiss = { mostrarDialogoEnviar = false },
            onEnviarNube = {
                enviarANube(contexto, archivosAProcesar)
            },
            onCopiarCarpeta = {
                launcherSeleccionarCarpetaCopiar.launch(null)
            },
            onMoverCarpeta = {
                launcherSeleccionarCarpetaMover.launch(null)
            }
        )

        // Diálogo de confirmación de eliminación / papelera
        DialogoConfirmarEliminar(
            mostrar = mostrarDialogoEliminar,
            cantidad = archivosAProcesar.size,
            onDismiss = {
                mostrarDialogoEliminar = false
                archivoAEliminarIndividual = null
            },
            onEnviarPapelera = {
                ejecutarPapelera(
                    contexto = contexto,
                    archivos = archivosAProcesar,
                    launcherIntentSender = launcherIntentSender,
                    onExitoDirecto = { lista ->
                        viewModel.eliminarSeleccionadosLocales(lista.map { it.id }.toSet())
                    }
                )
            },
            onEliminarPermanente = {
                ejecutarEliminacionPermanente(
                    contexto = contexto,
                    archivos = archivosAProcesar,
                    launcherIntentSender = launcherIntentSender,
                    onExitoDirecto = { lista ->
                        viewModel.eliminarSeleccionadosLocales(lista.map { it.id }.toSet())
                    }
                )
            }
        )

        // Visor de imagen ampliable con galería paginada y marcado continuo
        if (imagenSeleccionada != null && listaImagenesActuales.isNotEmpty()) {
            VisorImagenDialog(
                listaArchivos = listaImagenesActuales,
                indiceInicial = indiceImagenSeleccionada,
                seleccionados = estado.seleccionados,
                onToggleSeleccion = { viewModel.toggleSeleccion(it.id) },
                onDismiss = {
                    imagenSeleccionada = null
                    indiceImagenSeleccionada = -1
                },
                onCompartir = { compartirArchivo(contexto, it) },
                onAbrirExterno = { abrirArchivo(contexto, it) }
            )
        }

        // Reproductor de video en pantalla completa
        ReproductorVideoDialog(
            archivo = videoSeleccionado,
            estaSeleccionado = videoSeleccionado?.id in estado.seleccionados,
            onToggleSeleccion = { viewModel.toggleSeleccion(it.id) },
            onDismiss = { videoSeleccionado = null },
            onCompartir = { compartirArchivo(contexto, it) },
            onAbrirExterno = { abrirArchivo(contexto, it) }
        )

        // Reproductor de audio/música interactivo
        ReproductorAudioDialog(
            archivo = audioSeleccionado,
            estaSeleccionado = audioSeleccionado?.id in estado.seleccionados,
            onToggleSeleccion = { viewModel.toggleSeleccion(it.id) },
            onDismiss = { audioSeleccionado = null },
            onCompartir = { compartirArchivo(contexto, it) },
            onAbrirExterno = { abrirArchivo(contexto, it) }
        )
    }
}

private fun transferirArchivosACarpeta(
    contexto: Context,
    archivos: List<Archivo>,
    treeUri: Uri,
    esMover: Boolean,
    onFin: () -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val docDir = DocumentFile.fromTreeUri(contexto, treeUri)
            if (docDir == null || !docDir.canWrite()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(contexto, "No se puede escribir en la carpeta seleccionada", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            var transferidos = 0
            for (archivo in archivos) {
                try {
                    val nuevoDoc = docDir.createFile(archivo.mimeType ?: "application/octet-stream", archivo.nombre) ?: continue
                    contexto.contentResolver.openInputStream(archivo.uri)?.use { input ->
                        contexto.contentResolver.openOutputStream(nuevoDoc.uri)?.use { output ->
                            input.copyTo(output)
                            transferidos++
                        }
                    }
                } catch (e: Exception) {
                    // ignorar fallos individuales
                }
            }

            withContext(Dispatchers.Main) {
                val accion = if (esMover) "movidos" else "copiados"
                Toast.makeText(contexto, "$transferidos archivos $accion con éxito a ${docDir.name ?: "la carpeta seleccionada"}", Toast.LENGTH_LONG).show()
                onFin()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(contexto, "Error al transferir: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun enviarANube(contexto: Context, archivos: List<Archivo>) {
    if (archivos.isEmpty()) return
    val uris = ArrayList(archivos.map { it.uri })
    val mimeGeneral = if (archivos.all { it.tipo == com.buscadorandroid.app.domain.model.TipoArchivo.IMAGEN }) "image/*" else "*/*"

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = archivos.first().mimeType ?: mimeGeneral
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeGeneral
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    contexto.startActivity(Intent.createChooser(intent, "Subir a la nube (Drive, OneDrive, Dropbox...)"))
}

private fun abrirArchivo(contexto: Context, archivo: Archivo) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(archivo.uri, archivo.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(Intent.createChooser(intent, "Abrir con"))
    } catch (e: Exception) {
        Toast.makeText(contexto, "No hay app para abrir .${archivo.extension}", Toast.LENGTH_SHORT).show()
    }
}

private fun compartirArchivo(contexto: Context, archivo: Archivo) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = archivo.mimeType ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, archivo.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(intent, "Compartir ${archivo.nombre}"))
}

private fun compartirMultiplesArchivos(contexto: Context, archivos: List<Archivo>) {
    if (archivos.isEmpty()) return
    if (archivos.size == 1) {
        compartirArchivo(contexto, archivos.first())
        return
    }
    val uris = ArrayList(archivos.map { it.uri })
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(intent, "Compartir ${archivos.size} archivos"))
}

private fun copiarRuta(contexto: Context, archivo: Archivo) {
    val clipboard = ContextCompat.getSystemService(contexto, ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("ruta", archivo.ruta))
    Toast.makeText(contexto, "Ruta copiada", Toast.LENGTH_SHORT).show()
}

private fun ejecutarPapelera(
    contexto: Context,
    archivos: List<Archivo>,
    launcherIntentSender: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onExitoDirecto: (List<Archivo>) -> Unit
) {
    if (archivos.isEmpty()) return
    val uris = archivos.map { it.uri }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val pendingIntent = MediaStore.createTrashRequest(contexto.contentResolver, uris, true)
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            launcherIntentSender.launch(request)
        } catch (e: Exception) {
            Toast.makeText(contexto, "Error al preparar papelera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    } else {
        ejecutarEliminacionPermanente(contexto, archivos, launcherIntentSender, onExitoDirecto)
    }
}

private fun ejecutarEliminacionPermanente(
    contexto: Context,
    archivos: List<Archivo>,
    launcherIntentSender: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onExitoDirecto: (List<Archivo>) -> Unit
) {
    if (archivos.isEmpty()) return
    val uris = archivos.map { it.uri }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val pendingIntent = MediaStore.createDeleteRequest(contexto.contentResolver, uris)
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            launcherIntentSender.launch(request)
        } catch (e: Exception) {
            var borrados = 0
            for (archivo in archivos) {
                try {
                    val f = java.io.File(archivo.ruta)
                    if (f.exists() && f.delete()) {
                        borrados++
                    } else {
                        val n = contexto.contentResolver.delete(archivo.uri, null, null)
                        if (n > 0) borrados++
                    }
                } catch (ignored: Exception) {}
            }
            if (borrados > 0) {
                onExitoDirecto(archivos)
                Toast.makeText(contexto, "$borrados archivos eliminados", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        var borrados = 0
        for (archivo in archivos) {
            try {
                val f = java.io.File(archivo.ruta)
                if (f.exists() && f.delete()) {
                    borrados++
                } else {
                    val n = contexto.contentResolver.delete(archivo.uri, null, null)
                    if (n > 0) borrados++
                }
            } catch (ignored: Exception) {}
        }
        if (borrados > 0) {
            onExitoDirecto(archivos)
            Toast.makeText(contexto, "$borrados archivos eliminados", Toast.LENGTH_SHORT).show()
        }
    }
}
