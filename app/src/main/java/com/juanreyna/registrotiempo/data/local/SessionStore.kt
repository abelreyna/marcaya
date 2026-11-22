package com.juanreyna.registrotiempo.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.juanreyna.registrotiempo.domain.model.Rol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sesionDataStore by preferencesDataStore(name = "sesion_segura")

data class SesionLocal(val uid: String, val rol: Rol, val adminId: String)

/**
 * Persistencia de sesión: rol y adminId se guardan cifrados con Android Keystore
 * para impedir escalamiento de privilegios editando las preferencias en dispositivos rooteados.
 */
@Singleton
class SessionStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val kUid = stringPreferencesKey("uid")
    private val kRol = stringPreferencesKey("rol")
    private val kAdmin = stringPreferencesKey("admin_id")
    private val kRecordarme = booleanPreferencesKey("recordarme")

    suspend fun guardar(sesion: SesionLocal) {
        context.sesionDataStore.edit {
            it[kUid] = KeystoreCipher.cifrar(sesion.uid)
            it[kRol] = KeystoreCipher.cifrar(sesion.rol.name)
            it[kAdmin] = KeystoreCipher.cifrar(sesion.adminId)
        }
    }

    suspend fun leer(): SesionLocal? {
        val p: Preferences = context.sesionDataStore.data.first()
        val uid = p[kUid]?.let(KeystoreCipher::descifrar) ?: return null
        val rol = p[kRol]?.let(KeystoreCipher::descifrar)?.let { runCatching { Rol.valueOf(it) }.getOrNull() } ?: return null
        val admin = p[kAdmin]?.let(KeystoreCipher::descifrar) ?: return null
        return SesionLocal(uid, rol, admin)
    }

    suspend fun setRecordarme(valor: Boolean) {
        context.sesionDataStore.edit { it[kRecordarme] = valor }
    }

    suspend fun recordarme(): Boolean = context.sesionDataStore.data.first()[kRecordarme] ?: true

    suspend fun limpiar() {
        context.sesionDataStore.edit { it.clear() }
    }
}
