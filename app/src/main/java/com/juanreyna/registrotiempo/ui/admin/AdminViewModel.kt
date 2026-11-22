package com.juanreyna.registrotiempo.ui.admin

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
    val justificaciones: List<Justificacion> = emptyList()
) {
    val horasHoy get() = filas.sumOf { it.horasHoy }
    val costoHoy get() = filas.sumOf { it.costoHoy }
    val pendientes get() = justificaciones.count { it.estado == EstadoJustificacion.PENDIENTE }
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: FirestoreRepository
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
        Triple(UiAdmin(e, cols, emptyList(), alertas, justif), hoy, cols)
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

    fun guardarGeocerca(g: Geocerca) = ejecutar("Geocerca guardada") { repo.guardarGeocerca(adminId, g) }

    fun guardarColaborador(u: Usuario, cfg: ConfigPago, horario: Horario, activo: Boolean) =
        ejecutar("Datos de ${u.nombre} actualizados") { repo.actualizarColaborador(u.uid, cfg, horario, activo) }

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

    fun resolver(j: Justificacion, estado: EstadoJustificacion) =
        ejecutar("Justificación ${if (estado == EstadoJustificacion.APROBADA) "aprobada" else "rechazada"}") {
            repo.resolverJustificacion(j.id, estado)
        }

    fun cargarMes(mes: String = mesReporte.value) = viewModelScope.launch {
        mesReporte.value = mes
        resumenMes.value = null
        runCatching { repo.marcajesEmpresaMes(adminId, mes) }
            .onSuccess { resumenMes.value = ResumenMensual.construir(ui.value.colaboradores, it, System.currentTimeMillis()) }
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
