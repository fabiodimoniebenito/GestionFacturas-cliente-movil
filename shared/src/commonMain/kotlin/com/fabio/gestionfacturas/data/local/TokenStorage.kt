package com.fabio.gestionfacturas.data.local

import com.russhwolf.settings.Settings

/**
 * Where the session lives between runs: access token, refresh token, user id and email.
 * NOTE: Multiplatform Settings does not encrypt; secure storage is a known pending item.
 */
interface TokenStorage {
    fun guardarSesion(accessToken: String, refreshToken: String, usuarioId: Long, email: String)

    /** Refresh does not rotate the refresh token, so only the access token is replaced. */
    fun guardarAccessToken(accessToken: String)

    fun accessToken(): String?
    fun refreshToken(): String?
    fun usuarioId(): Long?
    fun email(): String?
    fun limpiar()
}

class SettingsTokenStorage(private val settings: Settings) : TokenStorage {

    override fun guardarSesion(accessToken: String, refreshToken: String, usuarioId: Long, email: String) {
        settings.putString(CLAVE_ACCESS, accessToken)
        settings.putString(CLAVE_REFRESH, refreshToken)
        settings.putLong(CLAVE_USUARIO_ID, usuarioId)
        settings.putString(CLAVE_EMAIL, email)
    }

    override fun guardarAccessToken(accessToken: String) {
        settings.putString(CLAVE_ACCESS, accessToken)
    }

    override fun accessToken(): String? = settings.getStringOrNull(CLAVE_ACCESS)

    override fun refreshToken(): String? = settings.getStringOrNull(CLAVE_REFRESH)

    override fun usuarioId(): Long? = settings.getLongOrNull(CLAVE_USUARIO_ID)

    override fun email(): String? = settings.getStringOrNull(CLAVE_EMAIL)

    override fun limpiar() {
        settings.remove(CLAVE_ACCESS)
        settings.remove(CLAVE_REFRESH)
        settings.remove(CLAVE_USUARIO_ID)
        settings.remove(CLAVE_EMAIL)
    }

    private companion object {
        const val CLAVE_ACCESS = "auth.accessToken"
        const val CLAVE_REFRESH = "auth.refreshToken"
        const val CLAVE_USUARIO_ID = "auth.usuarioId"
        const val CLAVE_EMAIL = "auth.email"
    }
}
