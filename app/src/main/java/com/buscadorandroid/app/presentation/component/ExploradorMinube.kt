package com.buscadorandroid.app.presentation.component

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.navigation.compose.hiltViewModel
import com.buscadorandroid.app.domain.model.Archivo
import com.buscadorandroid.app.domain.model.EntradaSmb
import com.buscadorandroid.app.domain.model.MinubeConfig
import com.buscadorandroid.app.presentation.viewmodel.FaseMinube
import com.buscadorandroid.app.presentation.viewmodel.MinubeUiState
import com.buscadorandroid.app.presentation.viewmodel.MinubeViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploradorMinube(
    onCerrar: () -> Unit,
    viewModel: MinubeViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current
    var mostrarDialogoCarpeta by remember { mutableStateOf(false) }
    var pendienteDescarga by remember { mutableStateOf<List<EntradaSmb>>(emptyList()) }
    var esMovimiento by remember { mutableStateOf(false) }
    var entradaPorEliminar by remember { mutableStateOf<EntradaSmb?>(null) }

    val launcherCarpetaLocal = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && pendienteDescarga.isNotEmpty()) {
            viewModel.descargar(uri, pendienteDescarga, esMovimiento)
            pendienteDescarga = emptyList()
            esMovimiento = false
        }
    }

    fun iniciarDescarga(entradas: List<EntradaSmb>, mover: Boolean) {
        if (entradas.isEmpty()) return
        pendienteDescarga = entradas
        esMovimiento = mover
        launcherCarpetaLocal.launch(null)
    }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            Toast.makeText(contexto, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("MiNube (SMB)")
                                val ruta = estado.rutaActual
                                Text(
                                    if (ruta.isBlank()) "Raíz del recurso" else ruta,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (estado.rutaActual.isNotBlank()) viewModel.subirNivel()
                                    else onCerrar()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        },
                        actions = {
                            if (estado.rutaActual.isNotBlank()) {
                                IconButton(onClick = { viewModel.subirNivel() }) {
                                    Icon(Icons.Filled.ArrowUpward, "Subir un nivel")
                                }
                            }
                            if (estado.fase == FaseMinube.CONECTADO) {
                                IconButton(onClick = { mostrarDialogoCarpeta = true }) {
                                    Icon(Icons.Filled.CreateNewFolder, "Crear carpeta")
                                }
                            }
                            IconButton(onClick = onCerrar) {
                                Icon(Icons.Filled.Close, "Cerrar")
                            }
                        }
                    )
                },
                bottomBar = {
                    if (estado.fase == FaseMinube.CONECTADO) {
                        BarraAcciones(
                            estado = estado,
                            viewModel = viewModel,
                            onDescargarSeleccion = {
                                iniciarDescarga(
                                    estado.entradas.filter {
                                        it.ruta in estado.seleccion && !it.esDirectorio
                                    },
                                    false
                                )
                            }
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (estado.fase) {
                        FaseMinube.SIN_CONFIG -> PantallaConfiguracion(viewModel)
                        FaseMinube.CONECTANDO -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }

                        FaseMinube.ERROR -> PantallaError(estado, onCerrar)
                        FaseMinube.CONECTADO -> {
                            var textoBusqueda by remember { mutableStateOf("") }
                            LaunchedEffect(estado.buscando) {
                                if (!estado.buscando) textoBusqueda = ""
                            }
                            Column(Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = textoBusqueda,
                                    onValueChange = {
                                        textoBusqueda = it
                                        viewModel.alCambiarBusqueda(it)
                                    },
                                    label = { Text("Buscar en esta carpeta y subcarpetas") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    trailingIcon = {
                                        if (textoBusqueda.isNotEmpty()) {
                                            IconButton(onClick = {
                                                textoBusqueda = ""
                                                viewModel.alCambiarBusqueda("")
                                            }) { Icon(Icons.Filled.Close, "Limpiar búsqueda") }
                                        }
                                    }
                                )
                                ListaContenido(
                                    estado = estado,
                                    viewModel = viewModel,
                                    buscando = estado.buscando,
                                    modifier = Modifier.weight(1f),
                                    onDescargar = { iniciarDescarga(listOf(it), false) },
                                    onMover = { iniciarDescarga(listOf(it), true) },
                                    onEliminar = { entradaPorEliminar = it }
                                )
                            }
                        }
                    }

                    if (estado.progreso != null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoCarpeta) {
        var nombre by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { mostrarDialogoCarpeta = false },
            title = { Text("Crear carpeta") },
            text = {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre.isNotBlank()) {
                        viewModel.crearCarpeta(nombre.trim())
                        mostrarDialogoCarpeta = false
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCarpeta = false }) { Text("Cancelar") }
            }
        )
    }

    if (entradaPorEliminar != null) {
        val nombre = entradaPorEliminar!!.nombre
        AlertDialog(
            onDismissRequest = { entradaPorEliminar = null },
            title = { Text("Eliminar de MiNube") },
            text = {
                Text(
                    "¿Seguro que quieres eliminar '$nombre' de la carpeta de red? " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(entradaPorEliminar!!)
                    entradaPorEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { entradaPorEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PantallaConfiguracion(viewModel: MinubeViewModel) {
    val cfgInicial = viewModel.estado.value.cfg
    var host by remember { mutableStateOf(cfgInicial?.host ?: "") }
    var puerto by remember { mutableStateOf((cfgInicial?.puerto ?: 445).toString()) }
    var recurso by remember { mutableStateOf(cfgInicial?.recurso ?: "") }
    var dominio by remember { mutableStateOf(cfgInicial?.dominio ?: "") }
    var usuario by remember { mutableStateOf(cfgInicial?.usuario ?: "") }
    var contrasena by remember { mutableStateOf(cfgInicial?.contrasena ?: "") }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Conéctate a tu carpeta de red privada (SMB/CIFS) en la LAN.",
            style = MaterialTheme.typography.bodyMedium
        )
        Campo("Servidor (IP o nombre)", host) { host = it }
        Campo("Puerto", puerto, KeyboardType.Number) { puerto = it }
        Campo("Recurso compartido (ej. MiNube)", recurso) { recurso = it }
        Campo("Dominio (opcional)", dominio) { dominio = it }
        Campo("Usuario", usuario) { usuario = it }
        Campo("Contraseña", contrasena, KeyboardType.Password) { contrasena = it }

        Button(
            onClick = {
                val puertoInt = puerto.toIntOrNull() ?: 445
                viewModel.conectar(
                    MinubeConfig(
                        host = host.trim(),
                        puerto = puertoInt,
                        recurso = recurso.trim(),
                        dominio = dominio.trim(),
                        usuario = usuario.trim(),
                        contrasena = contrasena,
                        modoRendimiento = false
                    )
                )
            },
            enabled = host.isNotBlank() && recurso.isNotBlank()
        ) {
            Text("Guardar y conectar")
        }
        Text(
            "La conexión es solo dentro de tu red local (LAN). La app no envía datos a internet.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    tipo: KeyboardType = KeyboardType.Text,
    onCambio: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipo),
        visualTransformation = if (tipo == KeyboardType.Password)
            PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
private fun ListaContenido(
    estado: MinubeUiState,
    viewModel: MinubeViewModel,
    buscando: Boolean,
    modifier: Modifier = Modifier,
    onDescargar: (EntradaSmb) -> Unit,
    onMover: (EntradaSmb) -> Unit,
    onEliminar: (EntradaSmb) -> Unit
) {
    Column(modifier) {
        Text(
            if (buscando) "Resultados de la búsqueda:" else "Ruta: /${estado.rutaActual}",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
        if (estado.colaSubida.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Vas a subir ${estado.colaSubida.size} archivo(s) a esta carpeta. Pulsa 'Subir aquí'.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            items(estado.entradas, key = { it.ruta }) { e ->
                FilaEntrada(
                    e = e,
                    seleccionada = estado.seleccion.contains(e.ruta),
                    viewModel = viewModel,
                    buscando = buscando,
                    onDescargar = onDescargar,
                    onMover = onMover,
                    onEliminar = onEliminar
                )
            }
        }
    }
}

@Composable
private fun FilaEntrada(
    e: EntradaSmb,
    seleccionada: Boolean,
    viewModel: MinubeViewModel,
    buscando: Boolean,
    onDescargar: (EntradaSmb) -> Unit,
    onMover: (EntradaSmb) -> Unit,
    onEliminar: (EntradaSmb) -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                if (e.esDirectorio) {
                    if (buscando) viewModel.irA(e.ruta)
                    else viewModel.entrarCarpeta(e.nombre)
                } else {
                    viewModel.abrir(e)
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (e.esDirectorio) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.nombre, style = MaterialTheme.typography.bodyMedium)
            if (buscando) {
                val carpeta = e.ruta.substringBeforeLast('/', "")
                Text(
                    if (carpeta.isBlank()) "En la raíz" else "En: $carpeta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!e.esDirectorio) {
                Text(
                    Archivo.formatearTamano(e.tamanoBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Toca para abrir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Box {
            IconButton(onClick = { menuAbierto = true }) {
                Icon(Icons.Filled.MoreVert, "Más acciones")
            }
            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false }
            ) {
                if (!e.esDirectorio) {
                    DropdownMenuItem(
                        text = { Text("Descargar al teléfono") },
                        onClick = { menuAbierto = false; onDescargar(e) }
                    )
                    DropdownMenuItem(
                        text = { Text("Mover al teléfono (borra de la nube)") },
                        onClick = { menuAbierto = false; onMover(e) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Eliminar de MiNube") },
                    onClick = { menuAbierto = false; onEliminar(e) }
                )
            }
        }
        if (!e.esDirectorio) {
            Checkbox(
                checked = seleccionada,
                onCheckedChange = { viewModel.toggleSeleccion(e.ruta) }
            )
        }
    }
}

@Composable
private fun BarraAcciones(
    estado: MinubeUiState,
    viewModel: MinubeViewModel,
    onDescargarSeleccion: () -> Unit
) {
    Surface(tonalElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (estado.colaSubida.isNotEmpty()) {
                Button(onClick = { viewModel.subirCola() }, Modifier.weight(1f)) {
                    Icon(Icons.Filled.CloudUpload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Subir aquí (${estado.colaSubida.size})")
                }
            }
            if (estado.seleccion.isNotEmpty()) {
                OutlinedButton(onClick = onDescargarSeleccion, Modifier.weight(1f)) {
                    Icon(Icons.Filled.Download, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Descargar (${estado.seleccion.size})")
                }
            }
        }
    }
}

@Composable
private fun PantallaError(estado: MinubeUiState, onCerrar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(estado.mensaje ?: "Error de conexión", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCerrar) { Text("Volver") }
    }
}
