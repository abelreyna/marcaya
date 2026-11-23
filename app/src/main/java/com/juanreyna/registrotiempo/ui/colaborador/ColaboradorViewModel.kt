package com.juanreyna.registrotiempo.ui.colaborador

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanreyna.registrotiempo.data.repository.FirestoreRepository
import com.juanreyna.registrotiempo.domain.calc.JornadaCalculator
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.calc.ResumenMensual
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.MotivoJustificacion
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.Fechas
import com.juanreyna.registrotiempo.util.LocationValidator
import com.juanreyna.registrotiempo.util.ResultadoUbicacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiColaborador(
    val usuario: Usuario? = null,
    val empresa: Empresa? = null,
    val marcajesHoy: List<Marcaje> = emptyList(),
    val resumenMes: ResumenColaborador? = null,
    val justificaciones: List<Justificacion> = emptyList(),
    val ahora: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ColaboradorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: FirestoreRepository,
    private val ubicacion: LocationValidator
) : ViewModel() {

    private val uid: String = checkNotNull(savedState["uid"])
    val mesSeleccionado = MutableStateFlow(Fechas.mesActual())
    val ultimaUbicacion = MutableStateFlow<ResultadoUbicacion?>(null)
    val mensaje = MutableStateFlow<String?>(null)
    val procesando = MutableStateFlow(false)

    private val usuario = repo.observarUsuario(uid).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val empresa = usuario.filterNotNull().flatMapLatest { repo.observarEmpresa(it.adminId) }
    private val hoy = repo.observarMarcajesColaborador(uid, "fecha", Fechas.hoy())
    private val mes = mesSeleccionado.flatMapLatest { repo.observarMarcajesColaborador(uid, "mes", it) }
    private val justif = repo.observarJustificaciones("colaboradorId", uid)
    private val reloj = flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } }

    val ui: StateFlow<UiColaborador> = combine(usuario, empresa, hoy, mes, justif) { u, e, h, m, j ->
        UiColaborador(u, e, h, u?.let { ResumenMensual.construir(listOf(it), m, System.currentTimeMillis()).first() }, j)
    }.combine(reloj) { estado, t -> estado.copy(ahora = t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiColaborador())

    fun siguientesPermitidos(): Set<TipoMarcaje> =
        JornadaCalculator.siguientesPermitidos(ui.value.marcajesHoy.lastOrNull()?.tipo)

    fun verificarUbicacion() = viewModelScope.launch {
        val e = ui.value.empresa ?: return@launch
        ultimaUbicacion.value = ubicacion.validar(e.geocerca)
    }

    /**
     * Registra un marcaje: valida transición, anti-spam de 60 s, GPS real y geocerca.
     * El rostro se valida antes (pantalla de verificación) y solo se envía el booleano.
     */
    fun marcar(tipo: TipoMarcaje, validacionFacial: Boolean, bloqueadoPorVip: Boolean) = viewModelScope.launch {
        val u = ui.value.usuario ?: return@launch
        val e = ui.value.empresa ?: return@launch
        if (bloqueadoPorVip) { mensaje.value = "Periodo gratuito finalizado: activa el plan VIP para registrar nuevos marcajes."; return@launch }
        if (tipo !in siguientesPermitidos()) { mensaje.value = "Marcaje no permitido en este momento"; return@launch }
        if (tipo.requiereRostro && !validacionFacial) { mensaje.value = "Se requiere verificación facial"; return@launch }
        val ultimo = ui.value.marcajesHoy.mapNotNull { it.timestampMillis }.maxOrNull()
        if (ultimo != null && System.currentTimeMillis() - ultimo < 60_000) {
            mensaje.value = "Espera al menos 60 segundos entre marcajes"; return@launch
        }
        procesando.value = true
        try {
            when (val r = ubicacion.validar(e.geocerca).also { ultimaUbicacion.value = it }) {
                ResultadoUbicacion.GpsFalso -> {
                    mensaje.value = "Ubicación simulada detectada. Marcaje bloqueado y notificado al Administrador."
                    runCatching { repo.registrarAlerta(u, "GPS_FALSO", "Intento de marcaje ${tipo.etiqueta} con ubicación simulada") }
                }
                ResultadoUbicacion.SinSenal -> mensaje.value = "No se pudo obtener tu ubicación. Activa el GPS e inténtalo de nuevo."
                is ResultadoUbicacion.Valida -> {
                    repo.registrarMarcaje(u, tipo, r.latitud, r.longitud, r.distanciaMetros, r.dentro, validacionFacial && tipo.requiereRostro)
                    if (!r.dentro) runCatching {
                        repo.registrarAlerta(u, "FUERA_RANGO", "${tipo.etiqueta} a ${r.distanciaMetros.toInt()} m del centro de ${e.geocerca.nombre}")
                    }
                    mensaje.value = "${tipo.etiqueta} registrado" + if (!r.dentro) " (fuera de la geocerca: envía una justificación)" else ""
                }
            }
        } catch (ex: Exception) {
            mensaje.value = "Error al registrar: ${ex.message}"
        } finally {
            procesando.value = false
        }
    }

    fun enviarJustificacion(motivo: MotivoJustificacion, detalle: String, fecha: String) = viewModelScope.launch {
        val u = ui.value.usuario ?: return@launch
        runCatching { repo.enviarJustificacion(u, motivo, detalle, fecha) }
            .onSuccess { mensaje.value = "Justificación enviada al Administrador" }
            .onFailure { mensaje.value = "No se pudo enviar: ${it.message}" }
    }

    fun cambiarMes(delta: Int) = mesSeleccionado.update {
        if (delta < 0) Fechas.mesAnterior(it) else if (it == Fechas.mesActual()) it else Fechas.mesSiguiente(it)
    }

    fun consumirMensaje() { mensaje.value = null }
}
