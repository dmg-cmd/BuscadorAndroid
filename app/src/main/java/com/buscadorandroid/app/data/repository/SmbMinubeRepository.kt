package com.buscadorandroid.app.data.repository

import com.buscadorandroid.app.domain.model.EntradaSmb
import com.buscadorandroid.app.domain.model.MinubeConfig
import com.buscadorandroid.app.domain.repository.MinubeRepository
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import jcifs.smb.SmbFileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [MinubeRepository] usando jcifs (SMB/CIFS) para conectar
 * con una carpeta de red en la LAN, sin salir a internet.
 *
 * Notas de rendimiento: se amplían los buffers de red y se copia con bloques de 1 MB
 * para saturar el enlace de la LAN.
 */
@Singleton
class SmbMinubeRepository @Inject constructor() : MinubeRepository {

    init {
        // Buffers de red grandes para máxima velocidad en LAN
        System.setProperty("jcifs.smb.client.rcv_buf_size", "131072")
        System.setProperty("jcifs.smb.client.snd_buf_size", "131072")
        System.setProperty("jcifs.smb.client.timeout", "30000")
    }

    private fun construirContexto(cfg: MinubeConfig): CIFSContext {
        val base = SingletonContext.getInstance()
        val auth = NtlmPasswordAuthenticator(cfg.dominio, cfg.usuario, cfg.contrasena)
        return base.withCredentials(auth)
    }

    private fun urlBase(cfg: MinubeConfig): String =
        "smb://${cfg.host}:${cfg.puerto}/${cfg.recurso.trim('/')}/"

    private fun urlCarpeta(cfg: MinubeConfig, ruta: String): String {
        val base = urlBase(cfg)
        return if (ruta.isBlank()) base else "$base${ruta.trim('/')}/"
    }

    override suspend fun probarConexion(cfg: MinubeConfig): Result<Unit> = runCatching {
        val ctx = construirContexto(cfg)
        val raiz = SmbFile(urlBase(cfg), ctx)
        raiz.listFiles() // fuerza la conexión y valida credenciales
        Unit
    }

    override suspend fun listar(cfg: MinubeConfig, ruta: String): Result<List<EntradaSmb>> = runCatching {
        val ctx = construirContexto(cfg)
        val dir = SmbFile(urlCarpeta(cfg, ruta), ctx)
        dir.listFiles().map { f ->
            val nombreCrudo = f.name.trimEnd('/')
            EntradaSmb(
                nombre = nombreCrudo,
                ruta = if (ruta.isBlank()) nombreCrudo else "${ruta.trim('/')}/$nombreCrudo",
                esDirectorio = f.isDirectory,
                tamanoBytes = f.length(),
                fechaModificacion = f.lastModified()
            )
        }.sortedWith(compareBy({ !it.esDirectorio }, { it.nombre.lowercase() }))
    }

    override suspend fun buscar(cfg: MinubeConfig, rutaBase: String, termino: String): Result<List<EntradaSmb>> = runCatching {
        val ctx = construirContexto(cfg)
        val q = termino.lowercase()
        val resultados = mutableListOf<EntradaSmb>()
        val pendientes = ArrayDeque<String>().apply { add(rutaBase.trim('/')) }
        while (pendientes.isNotEmpty()) {
            val rel = pendientes.removeFirst()
            val url = if (rel.isBlank()) urlBase(cfg) else urlCarpeta(cfg, rel)
            val hijos = runCatching { SmbFile(url, ctx).listFiles() }.getOrDefault(emptyArray())
            for (f in hijos) {
                val nombreCrudo = f.name.trimEnd('/')
                if (nombreCrudo.isBlank()) continue
                val relHijo = if (rel.isBlank()) nombreCrudo else "$rel/$nombreCrudo"
                if (nombreCrudo.lowercase().contains(q)) {
                    resultados.add(
                        EntradaSmb(
                            nombre = nombreCrudo,
                            ruta = relHijo,
                            esDirectorio = f.isDirectory,
                            tamanoBytes = f.length(),
                            fechaModificacion = f.lastModified()
                        )
                    )
                }
                if (f.isDirectory) pendientes.add(relHijo)
                if (resultados.size >= 5000) break
            }
            if (resultados.size >= 5000) break
        }
        resultados.sortedWith(compareBy({ !it.esDirectorio }, { it.nombre.lowercase() }))
    }

    override suspend fun crearCarpeta(cfg: MinubeConfig, ruta: String, nombre: String): Result<Unit> = runCatching {
        val ctx = construirContexto(cfg)
        val nuevaUrl = "${urlCarpeta(cfg, ruta)}${nombre.trim('/')}/"
        val nueva = SmbFile(nuevaUrl, ctx)
        if (!nueva.exists()) nueva.mkdirs()
        Unit
    }

    override suspend fun subir(
        cfg: MinubeConfig,
        rutaCarpetaDestino: String,
        nombre: String,
        tamanoBytes: Long,
        entrada: InputStream,
        onProgreso: (bytes: Long, total: Long) -> Unit
    ): Result<Unit> = runCatching {
        val ctx = construirContexto(cfg)
        val destUrl = "${urlCarpeta(cfg, rutaCarpetaDestino)}${nombre.trim('/')}"
        val dest = SmbFile(destUrl, ctx)
        SmbFileOutputStream(dest, false).use { salida ->
            copiarConProgreso(entrada, salida, tamanoBytes, onProgreso)
        }
    }

    override suspend fun descargar(
        cfg: MinubeConfig,
        entrada: EntradaSmb,
        salida: OutputStream,
        onProgreso: (bytes: Long, total: Long) -> Unit
    ): Result<Unit> = runCatching {
        val ctx = construirContexto(cfg)
        val srcUrl = "${urlBase(cfg)}${entrada.ruta.trimStart('/')}"
        val src = SmbFile(srcUrl, ctx)
        SmbFileInputStream(src).use { entradaSmb ->
            copiarConProgreso(entradaSmb, salida, entrada.tamanoBytes, onProgreso)
        }
    }

    private fun copiarConProgreso(
        entrada: InputStream,
        salida: OutputStream,
        total: Long,
        onProgreso: (bytes: Long, total: Long) -> Unit
    ) {
        val buf = ByteArray(1024 * 1024) // 1 MB para máximo rendimiento en LAN
        var hecho = 0L
        var leido: Int
        while (entrada.read(buf).also { leido = it } != -1) {
            salida.write(buf, 0, leido)
            hecho += leido
            onProgreso(hecho, total)
        }
        salida.flush()
    }
}
