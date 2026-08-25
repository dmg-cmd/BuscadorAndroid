package com.buscadorandroid.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.buscadorandroid.app.domain.model.MinubeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda la configuración de MiNube (incluida la contraseña) cifrada en el dispositivo
 * mediante EncryptedSharedPreferences (AndroidKeyStore). La contraseña no se guarda en claro.
 */
@Singleton
class MinubeSettingsRepository @Inject constructor(
    @ApplicationContext private val contexto: Context
) {
    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "minube_creds",
            masterKeyAlias,
            contexto,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hayConfig(): Boolean {
        val host = prefs.getString(CLAVE_HOST, null)
        val recurso = prefs.getString(CLAVE_RECURSO, null)
        return !host.isNullOrBlank() && !recurso.isNullOrBlank()
    }

    fun obtener(): MinubeConfig? {
        val host = prefs.getString(CLAVE_HOST, null) ?: return null
        val recurso = prefs.getString(CLAVE_RECURSO, null) ?: return null
        if (host.isBlank() || recurso.isBlank()) return null
        return MinubeConfig(
            host = host,
            puerto = prefs.getInt(CLAVE_PUERTO, 445),
            recurso = recurso,
            dominio = prefs.getString(CLAVE_DOMINIO, "") ?: "",
            usuario = prefs.getString(CLAVE_USUARIO, "") ?: "",
            contrasena = prefs.getString(CLAVE_CONTRASENA, "") ?: "",
            modoRendimiento = prefs.getBoolean(CLAVE_RENDIMIENTO, false)
        )
    }

    fun guardar(cfg: MinubeConfig) {
        prefs.edit().apply {
            putString(CLAVE_HOST, cfg.host)
            putInt(CLAVE_PUERTO, cfg.puerto)
            putString(CLAVE_RECURSO, cfg.recurso)
            putString(CLAVE_DOMINIO, cfg.dominio)
            putString(CLAVE_USUARIO, cfg.usuario)
            putString(CLAVE_CONTRASENA, cfg.contrasena)
            putBoolean(CLAVE_RENDIMIENTO, cfg.modoRendimiento)
        }.apply()
    }

    fun borrar() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val CLAVE_HOST = "host"
        private const val CLAVE_PUERTO = "puerto"
        private const val CLAVE_RECURSO = "recurso"
        private const val CLAVE_DOMINIO = "dominio"
        private const val CLAVE_USUARIO = "usuario"
        private const val CLAVE_CONTRASENA = "contrasena"
        private const val CLAVE_RENDIMIENTO = "rendimiento"
    }
}
