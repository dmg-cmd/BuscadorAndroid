package com.buscadorandroid.app.domain.model

/**
 * Configuración de conexión a la carpeta de red MiNube (SMB/CIFS) en la LAN.
 */
data class MinubeConfig(
    val host: String,
    val puerto: Int = 445,
    val recurso: String,        // nombre del recurso compartido, p.ej. "MiNube"
    val dominio: String = "",
    val usuario: String,
    val contrasena: String,
    val modoRendimiento: Boolean = false
)
