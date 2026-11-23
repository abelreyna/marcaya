package com.juanreyna.registrotiempo.ui.admin

import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import com.juanreyna.registrotiempo.util.AvisosIncidencias
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
import com.juanreyna.registrotiempo.domain.calc.PlanillaPeruCalculator
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.calc.ReporteCostos
import com.juanreyna.registrotiempo.domain.calc.ResumenMensual
import com.juanreyna.registrotiempo.domain.model.Alerta
import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.EstadoColaborador
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.Geocerca
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.Fechas
import com.juanreyna.registrotiempo.util.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FilaMonitoreo(
    val usuario: Usuario,
    val estado: EstadoColaborador,
    val ultimo: Marcaje?,
    val horasHoy: Double,
    val costoHoy: Double
)

data class UiAdmin(
    val empresa: Empresa? = null,
    val colaboradores: List<Usuario> = emptyList(),
    val filas: List<FilaMonitoreo> = emptyList(),
    val alertas: List<Alerta> = emptyList(),
    val justificaciones: List<Justificacion> = emptyList(),
    val marcajesHoy: List<Marcaje> = emptyList()
) {
    val horasHoy get() = filas.sumOf { it.horasHoy }
    val costoHoy get() = filas.sumOf { it.costoHoy }
    val pendientes get() = justificaciones.count { it.estado == EstadoJustificacion.PENDIENTE }
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: FirestoreRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val adminId: String = checkNotNull(savedState["uid"])
    val mensaje = MutableStateFlow<String?>(null)
    val resumenMes = MutableStateFlow<List<ResumenColaborador>?>(null)
    val mesReporte = MutableStateFlow(Fechas.mesActual())

    val admin = repo.observarUsuario(adminId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val reloj = flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } }

    val ui: StateFlow<UiAdmin> = combine(
        repo.observarEmpresa(adminId),
        repo.observarColaboradores(adminId),
        repo.observarMarcajesEmpresa(adminId, "fecha", Fechas.hoy()),
        repo.observarAlertasHoy(adminId),
        repo.observarJustificaciones("adminId", adminId)
    ) { e, cols, hoy, alertas, justif ->
        Triple(UiAdmin(e, cols, emptyList(), alertas, justif, hoy), hoy, cols)
    }.combine(reloj) { (base, hoy, cols), ahora ->
        val porColab = hoy.groupBy { it.colaboradorId }
        base.copy(filas = cols.map { u ->
            val ms = porColab[u.uid].orEmpty()
            val dia = ResumenMensual.diaDe(Fechas.hoy(), ms, ahora, u.horario)
            val horas = dia.minutosEfectivos / 60.0
            val costoHoy = PlanillaPeruCalculator.calcularDias(u.configPago, listOf(dia.horas)).let {
                if (u.configPago.modo == ModoPago.SIMPLE) it.totalIngresos else it.totalIngresos + it.aportesEmpleador.sumOf { a -> a.monto }
            }
            FilaMonitoreo(u, JornadaCalculator.estado(ms, u.horario), ms.lastOrNull(), horas, costoHoy)
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiAdmin())

    /** Marcajes del mes en curso de toda la empresa (para detectar faltas, tardanzas, etc.). */
    private val marcajesMesActual = MutableStateFlow<List<Marcaje>?>(null)

    /** Incidencias del mes de todo el equipo, con el estado de su justificación. */
    val incidencias: StateFlow<List<Incidencia>?> = combine(marcajesMesActual, ui) { ms, u ->
        if (ms == null || u.empresa == null) null
        else {
            // Los marcajes de hoy llegan en vivo; los del resto del mes se cargan al abrir
            val hoy = Fechas.hoy()
            val todos = ms.filter { it.fecha != hoy } + u.marcajesHoy
            u.colaboradores.flatMap { c -> DetectorIncidencias.detectar(c, todos, u.justificaciones, Fechas.mesActual(), System.currentTimeMillis()) }
                .sortedWith(compareByDescending<Incidencia> { it.fecha }.thenBy { it.colaboradorNombre })
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun cargarIncidencias() = viewModelScope.launch {
        runCatching { repo.marcajesEmpresaMes(adminId, Fechas.mesActual()) }
            .onSuccess { marcajesMesActual.value = it }
            .onFailure { mensaje.value = "No se pudieron cargar las incidencias: ${it.message}"; marcajesMesActual.value = emptyList() }
    }

    init {
        cargarIncidencias()
        // Notificación del sistema por cada alerta, incidencia o justificación nueva del equipo
        viewModelScope.launch {
            combine(ui, incidencias) { u, inc -> u to inc.orEmpty() }.collect { (u, inc) ->
                if (u.empresa == null) return@collect
                val avisos = buildList {
                    u.alertas.forEach { add(AvisosIncidencias.Aviso("alerta|${it.id}", tituloAlerta(it.tipo), "${it.colaboradorNombre}: ${it.detalle}")) }
                    inc.filter { it.estado == EstadoIncidencia.POR_JUSTIFICAR && it.tipo in AVISAR }
                        .forEach { add(AvisosIncidencias.Aviso(it.clave, "${it.titulo} · ${it.colaboradorNombre}", "${it.fecha} · ${it.detalle}")) }
                    u.justificaciones.filter { it.estado == EstadoJustificacion.PENDIENTE }
                        .forEach { add(AvisosIncidencias.Aviso("just|${it.id}", "Nueva justificación por revisar", "${it.colaboradorNombre} · ${it.incidencia?.etiqueta ?: it.motivo.etiqueta} (${it.fecha})")) }
                    sinIngresoHoy(u).forEach { add(AvisosIncidencias.Aviso("sining|${it.uid}|${Fechas.hoy()}", "${it.nombre} aún no marca ingreso", "Entrada programada a las ${it.horario.entrada}")) }
                }
                AvisosIncidencias.notificarNuevos(context, adminId, avisos)
            }
        }
    }

    /** Colaboradores que dejaron la empresa: sus marcajes siguen saliendo en los reportes. */
    private val exColaboradores = repo.observarExColaboradores(adminId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun desvincular(u: Usuario) = ejecutar("${u.nombre} fue desvinculado del equipo") { repo.desvincularColaborador(u) }

    fun eliminarColaborador(u: Usuario, borrarHistorial: Boolean) =
        ejecutar(if (borrarHistorial) "Cuenta e historial de ${u.nombre} eliminados" else "Cuenta de ${u.nombre} eliminada") {
            repo.eliminarColaboradorPorAdmin(u, borrarHistorial)
        }

    fun guardarGeocerca(g: Geocerca) = ejecutar("Geocerca guardada") { repo.guardarGeocerca(adminId, g) }

    fun guardarColaborador(u: Usuario, cfg: ConfigPago, horario: Horario, activo: Boolean, tipoDocumento: TipoDocumento, dni: String) =
        ejecutar("Datos de ${u.nombre} actualizados") { repo.actualizarColaborador(u.uid, cfg, horario, activo, tipoDocumento, dni) }

    /** Pestaña Horarios: aplica un mismo horario a los colaboradores seleccionados. */
    fun aplicarHorario(uids: List<String>, horario: Horario) =
        ejecutar("Horario aplicado a ${uids.size} ${if (uids.size == 1) "colaborador" else "colaboradores"}") {
            repo.aplicarHorario(uids, horario)
        }

    /** Aplica la modalidad de pago global a todo el equipo conservando tarifas individuales. */
    fun aplicarModoGlobal(modo: ModoPago, moneda: String) = ejecutar("Modalidad aplicada a todo el equipo") {
        repo.guardarModoGlobal(adminId, modo, moneda)
        ui.value.colaboradores.forEach { u ->
            repo.actualizarColaborador(u.uid, u.configPago.copy(modo = modo), u.horario, u.activo)
        }
    }

    /** Registra la salida de un colaborador con la hora elegida por el empleador (jornada de hoy). */
    fun marcarSalida(f: FilaMonitoreo, hhmm: String) {
        val salida = runCatching { Fechas.millisDe(Fechas.hoy(), hhmm) }.getOrNull() ?: return
        val ultimo = f.ultimo?.timestampMillis ?: 0L
        when {
            salida > System.currentTimeMillis() -> mensaje.value = "La hora de salida no puede ser posterior a la hora actual"
            salida <= ultimo -> mensaje.value = "La salida debe ser después del último marcaje (${Fechas.hhmm(ultimo)})"
            else -> ejecutar("Salida de ${f.usuario.nombre} registrada a las $hhmm") {
                repo.registrarSalidaPorAdmin(adminId, f.usuario, Fechas.hoy(), salida)
            }
        }
    }

    /**
     * Salida de un día anterior que el colaborador olvidó marcar (incidencia "No marcó salida").
     * La hora debe ser posterior a su último marcaje de ese día.
     */
    fun marcarSalidaDia(i: Incidencia, hhmm: String) = viewModelScope.launch {
        val u = ui.value.colaboradores.firstOrNull { it.uid == i.colaboradorId } ?: run { mensaje.value = "Colaborador no encontrado"; return@launch }
        val salida = runCatching { Fechas.millisDe(i.fecha, hhmm) }.getOrNull() ?: return@launch
        val ultimo = runCatching { repo.marcajesDelDia(adminId, u.uid, i.fecha) }.getOrDefault(emptyList())
            .mapNotNull { it.timestampMillis }.maxOrNull() ?: 0L
        when {
            salida > System.currentTimeMillis() -> mensaje.value = "La hora de salida no puede ser posterior a la hora actual"
            salida <= ultimo -> mensaje.value = "La salida debe ser después del último marcaje de ese día (${Fechas.hhmm(ultimo)})"
            else -> ejecutar("Salida de ${u.nombre} del ${i.fecha} registrada a las $hhmm") {
                repo.registrarSalidaPorAdmin(adminId, u, i.fecha, salida)
                cargarIncidencias()
            }
        }
    }

    /** Activa o quita el domingo como día laborable para todo el equipo (conserva el resto de cada horario). */
    fun habilitarDomingo(habilitar: Boolean) = ejecutar(if (habilitar) "Marcaje en domingo habilitado" else "Marcaje en domingo deshabilitado") {
        ui.value.colaboradores.forEach { u ->
            val dias = if (habilitar) (u.horario.diasLaborables + 7).distinct().sorted() else u.horario.diasLaborables - 7
            if (dias.isNotEmpty() && dias != u.horario.diasLaborables) repo.actualizarDiasLaborables(u, dias)
        }
    }

    fun resolver(j: Justificacion, estado: EstadoJustificacion) =
        ejecutar("Justificación ${if (estado == EstadoJustificacion.APROBADA) "aprobada" else "no autorizada"}") {
            repo.resolverJustificacion(j.id, estado)
        }

    fun cargarMes(mes: String = mesReporte.value) = viewModelScope.launch {
        mesReporte.value = mes
        resumenMes.value = null
        runCatching { repo.marcajesEmpresaMes(adminId, mes) }
            .onSuccess { ms ->
                // Activos + ex colaboradores que tienen marcajes en el mes (desvinculados o con cuenta eliminada)
                val activos = ui.value.colaboradores
                val conMarcajes = ms.map { it.colaboradorId }.toSet()
                val ex = exColaboradores.value.filter { x -> x.uid in conMarcajes && activos.none { it.uid == x.uid } }
                resumenMes.value = ResumenMensual.construir(activos + ex, ms, System.currentTimeMillis())
            }
            .onFailure { mensaje.value = "No se pudo cargar el mes: ${it.message}"; resumenMes.value = emptyList() }
    }

    fun exportar(context: Context, formato: String) = viewModelScope.launch {
        val e = ui.value.empresa ?: return@launch
        val datos = resumenMes.value ?: return@launch
        val mes = mesReporte.value
        runCatching {
            withContext(Dispatchers.IO) {
                when (formato) {
                    "PDF" -> ReportExporter.asistenciaSunafilPdf(context, e, datos, Fechas.etiquetaMes(mes)) to "application/pdf"
                    "CSV" -> ReportExporter.planillaCsv(context, e, datos, mes) to "text/csv"
                    "COSTOS_PDF" -> ReportExporter.costosPdf(context, e, ReporteCostos.construir(datos), Fechas.etiquetaMes(mes)) to "application/pdf"
                    "COSTOS_CSV" -> ReportExporter.costosCsv(context, e, ReporteCostos.construir(datos), mes) to "text/csv"
                    else -> ReportExporter.plameJor(context, datos, mes) to "text/plain"
                }
            }
        }.onSuccess { (f, mime) -> ReportExporter.compartir(context, f, mime) }
            .onFailure { mensaje.value = "Error al exportar: ${it.message}" }
    }

    private fun ejecutar(ok: String, accion: suspend () -> Unit) = viewModelScope.launch {
        runCatching { accion() }
            .onSuccess { mensaje.value = ok }
            .onFailure { mensaje.value = "Error: ${it.message}" }
    }

    fun consumirMensaje() { mensaje.value = null }
}

/** Incidencias que generan notificación al empleador. */
private val AVISAR = setOf(
    TipoIncidencia.FALTA, TipoIncidencia.TARDANZA, TipoIncidencia.SALIDA_ANTICIPADA, TipoIncidencia.SALIDA_TARDIA,
    TipoIncidencia.EXCESO_DESCANSO, TipoIncidencia.DESCANSO_FUERA_HORARIO, TipoIncidencia.SIN_SALIDA
)

fun tituloAlerta(tipo: String): String = when (tipo) {
    "GPS_FALSO" -> "¡Ubicación simulada!"
    "FUERA_RANGO" -> "Marcaje fuera de la zona"
    else -> TipoIncidencia.entries.firstOrNull { it.name == tipo }?.etiqueta ?: "Alerta"
}

/** Colaboradores que hoy deben trabajar, ya pasó su hora de entrada + tolerancia y no marcaron ingreso. */
fun sinIngresoHoy(u: UiAdmin, ahora: Long = System.currentTimeMillis()): List<Usuario> {
    val minuto = Fechas.minutosDelDia(ahora)
    val dia = Fechas.diaSemanaIso(Fechas.hoy())
    return u.filas.filter { f ->
        val h = f.usuario.horario
        f.usuario.activo && f.estado == EstadoColaborador.SIN_MARCAR && h.trabajaEl(dia) &&
            minuto > Horario.minutos(h.entrada) + h.toleranciaMin
    }.map { it.usuario }
}
