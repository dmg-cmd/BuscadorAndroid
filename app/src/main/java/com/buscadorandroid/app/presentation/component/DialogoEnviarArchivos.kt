package com.buscadorandroid.app.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DialogoEnviarArchivos(
    mostrar: Boolean,
    cantidadArchivos: Int,
    onDismiss: () -> Unit,
    onEnviarNube: () -> Unit,
    onSubirMinube: () -> Unit,
    onCopiarCarpeta: () -> Unit,
    onMoverCarpeta: () -> Unit
) {
    if (!mostrar) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enviar $cantidadArchivos ${if (cantidadArchivos == 1) "archivo" else "archivos"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Elige el destino para los archivos marcados:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OpcionEnvioItem(
                    icono = Icons.Default.FolderShared,
                    titulo = "Subir a MiNube",
                    subtitulo = "Tu carpeta de red privada en la LAN (SMB)",
                    onClick = {
                        onDismiss()
                        onSubirMinube()
                    }
                )

                OpcionEnvioItem(
                    icono = Icons.Default.CloudUpload,
                    titulo = "Subir a la nube",
                    subtitulo = "Google Drive, OneDrive, Dropbox, etc.",
                    onClick = {
                        onDismiss()
                        onEnviarNube()
                    }
                )

                OpcionEnvioItem(
                    icono = Icons.Default.FolderShared,
                    titulo = "Copiar a carpeta local",
                    subtitulo = "Elige una carpeta en tu celular para copiarlos",
                    onClick = {
                        onDismiss()
                        onCopiarCarpeta()
                    }
                )

                OpcionEnvioItem(
                    icono = Icons.Default.DriveFileMove,
                    titulo = "Mover a carpeta local",
                    subtitulo = "Transfiere los archivos a otra carpeta",
                    onClick = {
                        onDismiss()
                        onMoverCarpeta()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun OpcionEnvioItem(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
