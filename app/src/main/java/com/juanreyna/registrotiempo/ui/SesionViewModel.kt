package com.juanreyna.registrotiempo.ui

import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import android.widget.Toast
import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.juanreyna.registrotiempo.data.local.SesionLocal
import com.juanreyna.registrotiempo.data.local.SessionStore
import com.juanreyna.registrotiempo.data.remote.FirebaseState
import com.juanreyna.registrotiempo.data.repository.AuthRepository
import com.juanreyna.registrotiempo.data.repository.FirestoreRepository
import com.juanreyna.registrotiempo.data.repository.RemoteConfigRepository
import com.juanreyna.registrotiempo.domain.model.Rol
import com.juanreyna.registrotiempo.domain.vip.AccessStatus
import com.juanreyna.registrotiempo.domain.vip.VipAccessManager
import com.juanreyna.registrotiempo.domain.vip.VipConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface Destino {
    data object Cargando : Destino
    data object FirebaseSinConfigurar : Destino
    data object Login : Destino
    data object Registro : Destino
    data class Admin(val uid: String) : Destino
    data class Colaborador(val uid: String) : Destino
    /** Colaborador sin empresa (fue desvinculado): puede vincularse con otro código o eliminar su cuenta. */
    data class SinEmpresa(val uid: String) : Destino
}

data class EstadoSesion(
    val destino: Destino = Destino.Cargando,
    val vipConfig: VipConfig = VipConfig(),
    val acceso: AccessStatus = AccessStatus.GRANTED,
    val diasRestantes: Long = 0,
    val bannerCerrado: Boolean = false,
    val error: String? = null,
    val procesando: Boolean = false,
    /** Texto de avance mientras se elimina una cuenta. */
    val progreso: String? = null
)

/**
 * Decide la pantalla inicial: si hay sesión persistente (Firebase Auth + DataStore cifrado)
 * abre directamente el Panel del Administrador o la Pantalla de Marcaje.
 */
@HiltViewModel
class SesionViewModel @Inject constructor(
    app: Application,
    private val auth: AuthRepository,
    private val repo: FirestoreRepository,
    private val remote: RemoteConfigRepository,
    private val store: SessionStore
) : AndroidViewModel(app) {

    private val _estado = MutableStateFlow(EstadoSesion())
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    init { resolver(arranqueEnFrio = true) }

    fun resolver(arranqueEnFrio: Boolean = false) = viewModelScope.launch {
        if (!FirebaseState.estaConfigurado(getApplication())) {
            _estado.update { it.copy(destino = Destino.FirebaseSinConfigurar) }; return@launch
        }
        var usuario = auth.usuarioActual
        // Sin "Recordarme": la sesión termina al cerrar la app
        if (usuario != null && arranqueEnFrio && !store.recordarme()) {
            auth.cerrarSesion(null); store.limpiar(); usuario = null
        }
        if (usuario == null) {
            _estado.update { it.copy(destino = Destino.Login) }; return@launch
        }
        // Ruta rápida: caché cifrada local (funciona sin conexión)
        store.leer()?.takeIf { it.uid == usuario.uid }?.let { s ->
            irA(s.rol, s.uid, s.adminId)
        }
        // Verificación contra Firestore
        val consulta = runCatching { repo.obtenerUsuario(usuario.uid) }
        // Sin conexión: se mantiene la ruta de la caché local
        if (consulta.isFailure) {
            if (store.leer() == null) _estado.update { it.copy(destino = Destino.Registro) }
            return@launch
        }
        val perfil = consulta.getOrNull()
        if (perfil == null) {
            // El perfil ya no existe (p. ej. el empleador eliminó la cuenta): se registra de nuevo
            store.leer()?.let { s ->
                if (s.adminId.isNotBlank()) runCatching { FirebaseMessaging.getInstance().unsubscribeFromTopic("empresa_${s.adminId}") }
                store.limpiar()
                Toast.makeText(getApplication(), "Tu perfil fue eliminado. Puedes registrarte de nuevo.", Toast.LENGTH_LONG).show()
            }
            _estado.update { it.copy(destino = Destino.Registro) }
            return@launch
        }
        store.guardar(SesionLocal(perfil.uid, perfil.rol, perfil.adminId))
        irA(perfil.rol, perfil.uid, perfil.adminId)
    }

    private suspend fun irA(rol: Rol, uid: String, adminId: String) {
        val destino = when {
            rol == Rol.ADMINISTRADOR -> Destino.Admin(uid)
            adminId.isBlank() -> Destino.SinEmpresa(uid)
            else -> Destino.Colaborador(uid)
        }
        _estado.update { it.copy(destino = destino) }
        if (adminId.isBlank()) return
        evaluarVip(adminId)
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic("empresa_$adminId") }
    }

    private suspend fun evaluarVip(adminId: String) {
        val cfg = runCatching { remote.cargar() }.getOrDefault(VipConfig())
        val empresa = runCatching { repo.obtenerEmpresa(adminId) }.getOrNull() ?: return
        val ahora = System.currentTimeMillis()
        _estado.update {
            it.copy(
                vipConfig = cfg,
                acceso = VipAccessManager.evaluateCompanyAccess(empresa.plan, empresa.fechaCreacionMillis, ahora, cfg),
                diasRestantes = VipAccessManager.diasRestantes(empresa.fechaCreacionMillis, ahora, cfg)
            )
        }
    }

    fun iniciarConGoogle(activity: Activity, recordarme: Boolean) = viewModelScope.launch {
        _estado.update { it.copy(procesando = true, error = null) }
        runCatching {
            store.setRecordarme(recordarme)
            auth.iniciarConGoogle(activity)
        }.onSuccess {
            _estado.update { it.copy(procesando = false, destino = Destino.Cargando) }
            resolver()
        }.onFailure { e ->
            _estado.update { it.copy(procesando = false, error = e.message ?: "No se pudo iniciar sesión") }
        }
    }

    fun registrarAdministrador(nombre: String, empresa: String) = registrar {
        val u = auth.usuarioActual!!
        repo.registrarAdministrador(u.uid, u.email.orEmpty(), nombre, empresa)
    }

    fun registrarColaborador(nombre: String, tipoDocumento: TipoDocumento, dni: String, telefono: String, codigo: String) = registrar {
        val u = auth.usuarioActual!!
        repo.registrarColaborador(u.uid, u.email.orEmpty(), nombre, tipoDocumento, dni, telefono, codigo)
    }

    private fun registrar(accion: suspend () -> Unit) = viewModelScope.launch {
        _estado.update { it.copy(procesando = true, error = null) }
        runCatching { accion() }
            .onSuccess { _estado.update { it.copy(procesando = false) }; resolver() }
            .onFailure { e -> _estado.update { it.copy(procesando = false, error = e.message ?: "Error al registrar") } }
    }

    /** Colaborador sin empresa: se vincula a otra con un nuevo Código de Administrador. */
    fun revincular(codigo: String) = registrar {
        repo.revincularColaborador(auth.usuarioActual!!.uid, codigo)
    }

    /**
     * Elimina la cuenta del usuario actual (pide de nuevo la cuenta de Google):
     * - Administrador: borra la empresa y todos sus datos, desvincula al equipo y elimina su usuario.
     * - Colaborador: borra su perfil (sus marcajes quedan para el empleador) y elimina su usuario.
     * Después puede volver a registrarse con la misma cuenta de Google desde cero.
     */
    fun eliminarCuenta(activity: Activity) = viewModelScope.launch {
        _estado.update { it.copy(procesando = true, error = null, progreso = "Confirmando tu cuenta de Google…") }
        runCatching {
            val u = auth.usuarioActual ?: error("No hay sesión activa")
            auth.reautenticar(activity)
            val perfil = repo.obtenerUsuario(u.uid)
            if (perfil != null) {
                if (perfil.rol == Rol.ADMINISTRADOR) {
                    repo.eliminarEmpresa(u.uid) { p -> _estado.update { it.copy(progreso = p) } }
                    runCatching { FirebaseMessaging.getInstance().unsubscribeFromTopic("empresa_${u.uid}") }
                } else {
                    _estado.update { it.copy(progreso = "Borrando tu perfil…") }
                    repo.eliminarPerfilColaborador(perfil)
                    if (perfil.adminId.isNotBlank()) runCatching { FirebaseMessaging.getInstance().unsubscribeFromTopic("empresa_${perfil.adminId}") }
                }
            }
            _estado.update { it.copy(progreso = "Eliminando tu usuario…") }
            auth.eliminarUsuario(activity)
            store.limpiar()
            getApplication<Application>().getSharedPreferences("avisos_incidencias", 0).edit().clear().apply()
            getApplication<Application>().getSharedPreferences("notif_leidas", 0).edit().clear().apply()
        }.onSuccess {
            Toast.makeText(getApplication(), "Tu cuenta fue eliminada. Puedes volver a registrarte cuando quieras.", Toast.LENGTH_LONG).show()
            _estado.update { EstadoSesion(destino = Destino.Login) }
        }.onFailure { e ->
            _estado.update { it.copy(procesando = false, progreso = null, error = "No se pudo eliminar la cuenta: ${e.message}") }
        }
    }

    fun nombreGoogle(): String = auth.usuarioActual?.displayName.orEmpty()

    fun cerrarBanner() = _estado.update { it.copy(bannerCerrado = true) }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    fun cerrarSesion(activity: Activity?) = viewModelScope.launch {
        auth.cerrarSesion(activity)
        store.limpiar()
        _estado.update { EstadoSesion(destino = Destino.Login) }
    }
}
