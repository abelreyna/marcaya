package com.juanreyna.registrotiempo.ui.perfil

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanreyna.registrotiempo.data.repository.AuthRepository
import com.juanreyna.registrotiempo.data.repository.FirestoreRepository
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.FotoPerfil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class UiPerfil(
    val usuario: Usuario? = null,
    val empresa: Empresa? = null,
    val guardando: Boolean = false,
    val procesandoFoto: Boolean = false
)

/** Perfil básico del usuario conectado (Administrador o Colaborador). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PerfilViewModel @Inject constructor(
    auth: AuthRepository,
    private val repo: FirestoreRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val uid: String = auth.usuarioActual?.uid.orEmpty()
    private val guardando = MutableStateFlow(false)
    private val procesandoFoto = MutableStateFlow(false)
    val mensaje = MutableStateFlow<String?>(null)

    private val usuario = if (uid.isEmpty()) flowOf(null) else repo.observarUsuario(uid)

    private val empresa = usuario.filterNotNull().map { it.adminId }.distinctUntilChanged()
        .flatMapLatest { adminId -> repo.observarEmpresa(adminId) }
        .onStart { emit(null) }

    val ui: StateFlow<UiPerfil> = combine(usuario, empresa, guardando, procesandoFoto) { u, e, g, p ->
        UiPerfil(u, e, g, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiPerfil())

    fun guardarDatos(nombre: String, telefono: String) {
        if (uid.isEmpty() || guardando.value) return
        viewModelScope.launch {
            guardando.value = true
            runCatching { repo.actualizarDatosPerfil(uid, nombre.trim(), telefono.trim()) }
                .onSuccess { mensaje.value = "Perfil actualizado" }
                .onFailure { mensaje.value = "No se pudo guardar: ${it.message}" }
            guardando.value = false
        }
    }

    fun cambiarFoto(uri: Uri) {
        if (uid.isEmpty() || procesandoFoto.value) return
        viewModelScope.launch {
            procesandoFoto.value = true
            val foto = withContext(Dispatchers.Default) { FotoPerfil.desdeUri(context, uri) }
            if (foto == null) {
                mensaje.value = "No se pudo leer la imagen. Prueba con otra foto."
            } else {
                runCatching { repo.actualizarFotoPerfil(uid, foto) }
                    .onSuccess { mensaje.value = "Foto de perfil actualizada" }
                    .onFailure { mensaje.value = "No se pudo subir la foto: ${it.message}" }
            }
            procesandoFoto.value = false
        }
    }

    fun quitarFoto() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.actualizarFotoPerfil(uid, "") }
                .onSuccess { mensaje.value = "Foto eliminada" }
                .onFailure { mensaje.value = "No se pudo quitar la foto: ${it.message}" }
        }
    }

    fun consumirMensaje() { mensaje.value = null }
}
