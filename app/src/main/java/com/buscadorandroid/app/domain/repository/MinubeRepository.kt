package com.buscadorandroid.app.domain.repository

import com.buscadorandroid.app.domain.model.EntradaSmb
import com.buscadorandroid.app.domain.model.MinubeConfig
import java.io.InputStream
import java.io.OutputStream

/**
 * Acceso a la carpeta de red MiNube (SMB/CIFS) en la LAN.
 * Las operaciones reciben/entregan streams para no acoplar el repositorio al Context de Android.
 */
interface MinubeRepository {

    /** Verifica que la conexión y credenciales sean válidas. */
    suspend fun probarConexion(cfg: MinubeConfig): Result<Unit>

    /** Lista el contenido de una carpeta remota (ruta relativa al share; "" = raíz). */
    suspend fun listar(cfg: MinubeConfig, ruta: String): Result<List<EntradaSmb>>

    /** Crea una subcarpeta dentro de la ruta indicada. */
    suspend fun crearCarpeta(cfg: MinubeConfig, ruta: String, nombre: String): Result<Unit>

    /** Sube un archivo local a la carpeta remota. */
    suspend fun subir(
        cfg: MinubeConfig,
        rutaCarpetaDestino: String,
        nombre: String,
        tamanoBytes: Long,
        entrada: InputStream,
        onProgreso: (bytes: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit>

    /** Descarga una entrada remota a un stream de salida local. */
    suspend fun descargar(
        cfg: MinubeConfig,
        entrada: EntradaSmb,
        salida: OutputStream,
        onProgreso: (bytes: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit>
}
