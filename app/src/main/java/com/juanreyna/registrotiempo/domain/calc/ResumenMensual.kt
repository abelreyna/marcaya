package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.domain.model.EstadoIncidencia
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.Descuento
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.Fechas

data class DiaTrabajado(
    val fecha: String,
    val ingreso: Long?,
    val salida: Long?,
    val minutosEfectivos: Long,
    val minutosDescanso: Long,
    val tardanza: Boolean,
    val minutosTarde: Long = 0,
    val minutosExcesoDescanso: Long = 0,
    val minutosExtra: Long = 0,
    val minutosSalidaAnticipada: Long = 0,
    val fueraRango: Boolean,
    val validacionFacial: Boolean
) {
    /** Horas para la boleta: ordinarias (efectivas − extras) y extras. */
    val horas: HorasDia get() = HorasDia((minutosEfectivos - minutosExtra) / 60.0, minutosExtra / 60.0)
}

data class ResumenColaborador(
    val usuario: Usuario,
    val dias: List<DiaTrabajado>,
    val boleta: Boleta,
    /** Descuentos aplicados por el empleador en el mes (ya restados en la boleta). */
    val descuentos: List<Descuento> = emptyList()
) {
    val totalDescuentosEmpleador get() = descuentos.sumOf { it.monto }.r2()
    fun descuentosDel(fecha: String) = descuentos.filter { it.fecha == fecha }
    val horasTotales get() = dias.sumOf { it.minutosEfectivos } / 60.0
    val tardanzas get() = dias.count { it.tardanza }
    val minutosExtra get() = dias.sumOf { it.minutosExtra }
    val minutosExcesoDescanso get() = dias.sumOf { it.minutosExcesoDescanso }
}

object ResumenMensual {

    fun diasDe(marcajes: List<Marcaje>, ahoraMillis: Long, horario: Horario = Horario()): List<DiaTrabajado> =
        marcajes.groupBy { it.fecha }.toSortedMap().map { (fecha, delDia) -> diaDe(fecha, delDia, ahoraMillis, horario) }

    fun diaDe(fecha: String, delDia: List<Marcaje>, ahoraMillis: Long, horario: Horario): DiaTrabajado {
            val r = JornadaCalculator.resumir(delDia, ahoraMillis)
            val inc = JornadaCalculator.evaluar(delDia, r, horario.deFecha(fecha), ahoraMillis)
            val ingreso = delDia.firstOrNull { it.tipo == TipoMarcaje.INGRESO }?.timestampMillis
            return DiaTrabajado(
                fecha = fecha,
                ingreso = ingreso,
                salida = delDia.lastOrNull { it.tipo == TipoMarcaje.SALIDA }?.timestampMillis,
                minutosEfectivos = r.minutosEfectivos,
                minutosDescanso = r.minutosDescanso,
                tardanza = inc.tardanza,
                minutosTarde = inc.minutosTarde,
                minutosExcesoDescanso = inc.minutosExcesoDescanso,
                minutosExtra = inc.minutosExtra,
                minutosSalidaAnticipada = inc.minutosSalidaAnticipada,
                fueraRango = delDia.any { !it.dentroGeocerca },
                validacionFacial = delDia.filter { it.tipo.requiereRostro }.all { it.validacionFacial }
            )
    }

    /**
     * @param mes "yyyy-MM" del reporte (define la RMV y la UIT aplicables).
     * @param justificaciones para no descontar faltas ni tardanzas ya justificadas (Modo Avanzado).
     */
    fun construir(
        colaboradores: List<Usuario>,
        marcajesMes: List<Marcaje>,
        ahoraMillis: Long,
        mes: String = marcajesMes.firstOrNull()?.mes?.ifBlank { null } ?: Fechas.mesActual(),
        justificaciones: List<Justificacion> = emptyList(),
        parametros: ParametrosLegales = ParametrosLegales.delMes(mes),
        descuentos: List<Descuento> = emptyList()
    ): List<ResumenColaborador> {
        val porColaborador = marcajesMes.groupBy { it.colaboradorId }
        val descPorColaborador = descuentos.filter { it.mes == mes }.groupBy { it.colaboradorId }
        return colaboradores.map { u ->
            val propios = porColaborador[u.uid].orEmpty()
            val dias = diasDe(propios, ahoraMillis, u.horario)
            val periodo = if (u.configPago.modo == ModoPago.AVANZADO) periodoDe(u, propios, justificaciones, mes, ahoraMillis) else PeriodoPlanilla()
            val propiosDesc = descPorColaborador[u.uid].orEmpty().sortedBy { it.fecha }
            ResumenColaborador(
                usuario = u,
                dias = dias,
                boleta = conDescuentos(PlanillaPeruCalculator.calcularDias(u.configPago, dias.map { it.horas }, parametros, periodo), propiosDesc),
                descuentos = propiosDesc
            )
        }
    }

    /** Agrega a la boleta los descuentos del empleador como líneas "Descuento: concepto (dd/MM)": reducen el neto, no la base de aportes. */
    fun conDescuentos(b: Boleta, descuentos: List<Descuento>): Boleta =
        if (descuentos.isEmpty()) b
        else b.copy(descuentos = b.descuentos + descuentos.map {
            LineaBoleta("$PREFIJO_DESCUENTO${it.concepto} (${it.fecha.takeLast(2)}/${it.fecha.substring(5, 7)})", it.monto)
        })

    const val PREFIJO_DESCUENTO = "Descuento: "

    /**
     * Días computables desde la fecha de ingreso, faltas y minutos de tardanza no justificados del mes.
     * Una falta o tardanza en revisión o aún sin justificar se descuenta en la proyección; si el empleador
     * la aprueba, deja de descontarse.
     */
    fun periodoDe(u: Usuario, marcajes: List<Marcaje>, justificaciones: List<Justificacion>, mes: String, ahoraMillis: Long): PeriodoPlanilla {
        val ingreso = fechaIngreso(u)
        val diasComputables = when {
            ingreso == null || ingreso.take(7) < mes -> 30
            ingreso.take(7) > mes -> 0
            else -> (30 - (ingreso.takeLast(2).toInt() - 1)).coerceIn(0, 30)
        }
        val inc = DetectorIncidencias.detectar(u, marcajes, justificaciones, mes, ahoraMillis)
            .filter { it.estado != EstadoIncidencia.JUSTIFICADA }
        return PeriodoPlanilla(
            diasComputables = diasComputables,
            faltas = inc.count { it.tipo == TipoIncidencia.FALTA },
            minutosTardanza = inc.filter { it.tipo == TipoIncidencia.TARDANZA }.sumOf { it.minutos }
        )
    }

    /** Fecha de ingreso registrada por el empleador o, si no la hay, la fecha en que se vinculó a la empresa. */
    fun fechaIngreso(u: Usuario): String? =
        u.configPago.fechaIngreso.takeIf { Regex("\\d{4}-\\d{2}-\\d{2}").matches(it) } ?: u.creadoMillis?.let { Fechas.fechaDe(it) }
}
