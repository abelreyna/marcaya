package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.Usuario

/** Costo salarial de un día trabajado por un colaborador. */
data class CostoDia(
    val fecha: String,
    val colaborador: Usuario,
    val ingreso: Long?,
    val salida: Long?,
    val minutosOrdinarios: Long,
    val minutosExtra: Long,
    val minutosTarde: Long,
    val minutosExcesoDescanso: Long,
    val valorHora: Double,
    val montoOrdinario: Double,
    val montoExtra: Double
) {
    val minutosTrabajados get() = minutosOrdinarios + minutosExtra
    val total get() = (montoOrdinario + montoExtra).r2()
}

/** Resumen de costos de un colaborador en el mes. */
data class CostoColaborador(
    val colaborador: Usuario,
    val dias: List<CostoDia>,
    val boleta: Boleta
) {
    val minutosTrabajados get() = dias.sumOf { it.minutosTrabajados }
    val minutosExtra get() = dias.sumOf { it.minutosExtra }
    val pagadoPorDias get() = dias.sumOf { it.total }.r2()
}

/**
 * Reporte de costos (salarios): cuánto se pagó por día, por colaborador y en el mes.
 * - Monto del día = horas ordinarias × valor hora + horas extras
 *   (Modo Simple: extras a la misma tarifa, sin recargo; Modo Avanzado: +25 % las 2 primeras y +35 % desde la tercera).
 * - Valor hora: Modo Simple y Avanzado por hora = tarifa por hora; Avanzado mensual = sueldo base ÷ 240 h.
 * - Los totales del mes (asignación familiar, descuentos, aportes y provisiones) salen de la boleta.
 */
object ReporteCostos {

    fun valorHora(cfg: ConfigPago, p: ParametrosLegales = ParametrosLegales()): Double =
        if (cfg.modo == ModoPago.SIMPLE || cfg.pagoPorHora) cfg.tarifaPorHora else cfg.sueldoBase / p.horasMensualesBase

    fun costoDia(u: Usuario, d: DiaTrabajado, p: ParametrosLegales = ParametrosLegales()): CostoDia {
        val cfg = u.configPago
        val vh = valorHora(cfg, p)
        val (r1, r2) = if (cfg.modo == ModoPago.SIMPLE) 0.0 to 0.0 else 25.0 to 35.0
        val extraH = d.minutosExtra / 60.0
        val tramo1 = extraH.coerceAtMost(2.0)
        val tramo2 = (extraH - 2.0).coerceAtLeast(0.0)
        val ordinariosMin = (d.minutosEfectivos - d.minutosExtra).coerceAtLeast(0)
        return CostoDia(
            fecha = d.fecha,
            colaborador = u,
            ingreso = d.ingreso,
            salida = d.salida,
            minutosOrdinarios = ordinariosMin,
            minutosExtra = d.minutosExtra,
            minutosTarde = d.minutosTarde,
            minutosExcesoDescanso = d.minutosExcesoDescanso,
            valorHora = vh,
            montoOrdinario = (ordinariosMin / 60.0 * vh).r2(),
            montoExtra = (tramo1 * vh * (1 + r1 / 100) + tramo2 * vh * (1 + r2 / 100)).r2()
        )
    }

    fun construir(resumenes: List<ResumenColaborador>, p: ParametrosLegales = ParametrosLegales()): List<CostoColaborador> =
        resumenes.map { r -> CostoColaborador(r.usuario, r.dias.map { costoDia(r.usuario, it, p) }, r.boleta) }

    /** Todos los días del mes de todos los colaboradores, agrupados por fecha (más reciente primero). */
    fun porDia(costos: List<CostoColaborador>): List<Pair<String, List<CostoDia>>> =
        costos.flatMap { it.dias }.groupBy { it.fecha }.toSortedMap(compareByDescending { it })
            .map { (fecha, dias) -> fecha to dias.sortedBy { it.colaborador.nombre } }
}
