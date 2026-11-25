package com.juanreyna.registrotiempo.ui.colaborador

import com.juanreyna.registrotiempo.domain.calc.ReglasMarcaje
import kotlinx.coroutines.flow.map
import com.juanreyna.registrotiempo.util.AvisosIncidencias
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.domain.model.EstadoIncidencia
import com.juanreyna.registrotiempo.domain.model.Incidencia
import com.juanreyna.registrotiempo.domain.calc.DetectorIncidencias
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanreyna.registrotiempo.data.repository.FirestoreRepository
import com.juanreyna.registrotiempo.domain.calc.JornadaCalculator
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.calc.ResumenMensual
import com.juanreyna.registrotiempo.domain.model.Descuento
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
    /** Descuentos que le aplicó su empleador (empresa actual, todos los meses). */
    val descuentos: List<Descuento> = emptyList(),
    val ahora: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ColaboradorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: FirestoreRepository,
    private val ubicacion: LocationValidator,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val uid: String = checkNotNull(savedState["uid"])
    val mesSeleccionado = MutableStateFlow(Fechas.mesActual())
    val ultimaUbicacion = MutableStateFlow<ResultadoUbicacion?>(null)
    val mensaje = MutableStateFlow<String?>(null)
    val procesando = MutableStateFlow(false)

    private val usuario = repo.observarUsuario(uid).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** true cuando el servidor confirma que el perfil ya no existe (el empleador eliminó la cuenta). */
    val perfilEliminado = repo.observarExistePerfil(uid).map { !it }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val empresa = usuario.filterNotNull().flatMapLatest { if (it.adminId.isBlank()) flowOf(null) else repo.observarEmpresa(it.adminId) }
    private val hoy = repo.observarMarcajesColaborador(uid, "fecha", Fechas.hoy())
    private val mes = mesSeleccionado.flatMapLatest { repo.observarMarcajesColaborador(uid, "mes", it) }
    private val justif = repo.observarJustificaciones("colaboradorId", uid)
    private val reloj = flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } }
    private val descuentos = repo.observarDescuentos("colaboradorId", uid)

    val ui: StateFlow<UiColaborador> = combine(usuario, empresa, hoy, mes, justif) { u, e, h, m, j ->
        // Solo los datos de la empresa actual (si antes trabajó en otra, sus marcajes no se mezclan)
        val delAdmin = { mm: List<Marcaje> -> mm.filter { it.adminId == u?.adminId } }
        UiColaborador(u, e, delAdmin(h), u?.let { ResumenMensual.construir(
            listOf(it), delAdmin(m), System.currentTimeMillis(), mesSeleccionado.value, j.filter { x -> x.adminId == it.adminId }
        ).first() }, j.filter { it.adminId == u?.adminId })
    }.combine(descuentos) { estado, ds ->
        val propios = ds.filter { it.adminId == estado.usuario?.adminId }
        val r = estado.resumenMes
        estado.copy(
            descuentos = propios,
            resumenMes = r?.let {
                val delMes = propios.filter { d -> d.mes == mesSeleccionado.value }.sortedBy { d -> d.fecha }
                it.copy(boleta = ResumenMensual.conDescuentos(it.boleta, delMes), descuentos = delMes)
            }
        )
    }.combine(reloj) { estado, t -> estado.copy(ahora = t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiColaborador())

    /** Incidencias del mes actual (tardanzas, faltas, etc.) con el estado de su justificación. */
    private val mesActualMarcajes = repo.observarMarcajesColaborador(uid, "mes", Fechas.mesActual())
    val incidencias: StateFlow<List<Incidencia>> = combine(usuario.filterNotNull(), mesActualMarcajes, justif, reloj) { u, ms, js, ahora ->
        DetectorIncidencias.detectar(u, ms.filter { it.adminId == u.adminId }, js.filter { it.adminId == u.adminId }, Fechas.mesActual(), ahora)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Notificación del sistema por cada descuento que aplica el empleador
        viewModelScope.launch {
            combine(usuario.filterNotNull(), empresa, descuentos) { u, e, ds -> Triple(u, e, ds.filter { it.adminId == u.adminId }) }.collect { (_, e, ds) ->
                val mon = e?.moneda?.simbolo ?: "S/"
                AvisosIncidencias.notificarNuevos(context, "desc_$uid", ds.map { d ->
                    AvisosIncidencias.Aviso(
                        "desc|${d.id}", "Descuento aplicado: ${d.concepto}",
                        "−$mon ${"%,.2f".format(d.monto)} el ${Fechas.fechaCorta(d.fecha)} · ${d.motivo}"
                    )
                }, registrarSiVacio = true)
            }
        }
        // Notificación del sistema por cada incidencia nueva o justificación resuelta
        viewModelScope.launch {
            incidencias.collect { lista ->
                AvisosIncidencias.notificarNuevos(context, uid, lista.mapNotNull { i ->
                    when (i.estado) {
                        EstadoIncidencia.POR_JUSTIFICAR -> AvisosIncidencias.Aviso(i.clave, "Incidencia: ${i.titulo}", "${i.fecha} · ${i.detalle}. Envía tu justificación.")
                        EstadoIncidencia.JUSTIFICADA -> AvisosIncidencias.Aviso(i.clave + "|OK", "Justificación aprobada", "${i.tipo.etiqueta} del ${i.fecha}")
                        EstadoIncidencia.NO_AUTORIZADA -> AvisosIncidencias.Aviso(i.clave + "|NO", i.titulo, "Tu justificación del ${i.fecha} no fue autorizada")
                        EstadoIncidencia.EN_REVISION -> null
                    }
                })
            }
        }
    }

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
        if (tipo == TipoMarcaje.INGRESO && !u.horario.trabajaEl(Fechas.diaSemanaIso(Fechas.hoy()))) {
            mensaje.value = "Hoy no es día laborable en tu horario. Pide a tu administrador que habilite este día."; return@launch
        }
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
                    val incidencia = incidenciaAlMarcar(u, tipo)
                    incidencia?.let { (t, det) -> runCatching { repo.registrarAlerta(u, t.name, det) } }
                    mensaje.value = "${tipo.etiqueta} registrado" + when {
                        incidencia != null -> " · ${incidencia.first.etiqueta}: justifícala en la pestaña Justificar"
                        !r.dentro -> " (fuera de la geocerca: envía una justificación)"
                        else -> ""
                    }
                }
            }
        } catch (ex: Exception) {
            mensaje.value = "Error al registrar: ${ex.message}"
        } finally {
            procesando.value = false
        }
    }

    /** Incidencia del marcaje recién registrado frente al horario (tardanza, descanso fuera de horario, retorno tardío, salida antes o después). */
    private fun incidenciaAlMarcar(u: Usuario, tipo: TipoMarcaje): Pair<TipoIncidencia, String>? =
        ReglasMarcaje.evaluar(
            u.horario.deFecha(ui.value.marcajesHoy.firstOrNull()?.fecha ?: Fechas.hoy()), tipo, System.currentTimeMillis(),
            ui.value.marcajesHoy.lastOrNull { it.tipo == TipoMarcaje.INICIO_DESCANSO }?.timestampMillis
        )?.let { it.tipo to it.detalle }

    fun enviarJustificacion(motivo: MotivoJustificacion, detalle: String, fecha: String, incidencia: TipoIncidencia?) = viewModelScope.launch {
        val u = ui.value.usuario ?: return@launch
        runCatching { repo.enviarJustificacion(u, motivo, detalle, fecha, incidencia) }
            .onSuccess { mensaje.value = "Justificación enviada al Administrador" }
            .onFailure { mensaje.value = "No se pudo enviar: ${it.message}" }
    }

    fun cambiarMes(delta: Int) = mesSeleccionado.update {
        if (delta < 0) Fechas.mesAnterior(it) else if (it == Fechas.mesActual()) it else Fechas.mesSiguiente(it)
    }

    fun consumirMensaje() { mensaje.value = null }
}
