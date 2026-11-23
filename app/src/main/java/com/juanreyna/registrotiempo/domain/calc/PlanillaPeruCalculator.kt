package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.SistemaPension

/**
 * Parámetros legales del Perú. Los valores por defecto son los vigentes desde octubre de 2026;
 * [delMes] devuelve los de un mes concreto (la RMV y la UIT cambian con el tiempo).
 * Revisar cada año: RMV, UIT, prima de seguro, comisiones AFP y remuneración máxima asegurable (SBS, trimestral).
 */
data class ParametrosLegales(
    val rmv: Double = 1230.0,              // RMV: DS 015-2026-TR, desde el 01/10/2026 (antes S/ 1,130, DS 006-2024-TR)
    val uit: Double = 5500.0,              // UIT 2026: DS 301-2025-EF
    val diasMes: Int = 30,                 // mes laboral de 30 días
    val horasMensualesBase: Double = 240.0, // 30 días × 8 h (valor hora = remuneración ÷ 30 ÷ 8)
    val jornadaDiariaBase: Double = 8.0,
    val tasaOnp: Double = 0.13,
    val aporteAfp: Double = 0.10,
    val primaSeguroAfp: Double = 0.0137,   // prima de seguro previsional (igual en las 4 AFP)
    val remuneracionMaximaAsegurable: Double = 12732.70, // tope de la prima de seguro (SBS, oct–dic 2026)
    val comisionFlujo: Map<SistemaPension, Double> = mapOf(
        SistemaPension.AFP_INTEGRA to 0.0155,
        SistemaPension.AFP_PRIMA to 0.0160,
        SistemaPension.AFP_PROFUTURO to 0.0169,
        SistemaPension.AFP_HABITAT to 0.0147
    ),
    val tasaEssalud: Double = 0.09,
    val bonificacionExtraordinaria: Double = 0.09, // Ley 30334: lo que hubiera aportado a EsSalud por la gratificación
    val tramosQuinta: List<Pair<Double, Double>> = listOf( // (límite superior en UIT, tasa) — art. 53 LIR
        5.0 to 0.08, 20.0 to 0.14, 35.0 to 0.17, 45.0 to 0.20, Double.MAX_VALUE to 0.30
    ),
    val deduccionQuintaUit: Double = 7.0
) {
    companion object {
        /** Parámetros vigentes en el mes "yyyy-MM". */
        fun delMes(mes: String): ParametrosLegales = when {
            mes >= "2026-10" -> ParametrosLegales()
            mes >= "2026-01" -> ParametrosLegales(rmv = 1130.0)
            else -> ParametrosLegales(rmv = 1130.0, uit = 5350.0)
        }
    }
}

/**
 * Datos del mes para la boleta del Modo Avanzado (criterio laboral peruano: mes de 30 días).
 * @param diasComputables días del mes desde la fecha de ingreso (30 si ingresó antes del mes).
 * @param faltas inasistencias injustificadas: se descuenta 1/30 de la remuneración por cada una.
 * @param minutosTardanza tardanzas no justificadas: se descuentan al valor minuto.
 */
data class PeriodoPlanilla(
    val diasComputables: Int = 30,
    val faltas: Int = 0,
    val minutosTardanza: Long = 0
)

/** Horas de un día ya separadas en ordinarias y extras (según el horario del colaborador). */
data class HorasDia(val ordinarias: Double, val extra: Double)

data class LineaBoleta(val concepto: String, val monto: Double)

/** Boleta mensual proyectada (Modo Simple o Avanzado). */
data class Boleta(
    val modo: ModoPago,
    val horasOrdinarias: Double,
    val horasExtra25: Double,
    val horasExtra35: Double,
    val ingresos: List<LineaBoleta>,
    val descuentos: List<LineaBoleta>,
    val aportesEmpleador: List<LineaBoleta>,
    val provisiones: List<LineaBoleta>,
    /** Días pagados del mes (Modo Avanzado). */
    val diasLaborados: Int? = null,
    val faltas: Int = 0,
    /** Observaciones legales para el empleador (sueldo menor a la RMV, etc.). */
    val advertencias: List<String> = emptyList()
) {
    val totalIngresos get() = ingresos.sumOf { it.monto }.r2()
    val totalDescuentos get() = descuentos.sumOf { it.monto }.r2()
    val netoAPagar get() = (totalIngresos - totalDescuentos).r2()
    val costoEmpleador get() =
        (totalIngresos + aportesEmpleador.sumOf { it.monto } + provisiones.sumOf { it.monto }).r2()
}

/** Redondeo a céntimos, mitad hacia arriba (46.845 → 46.85), como en planilla. */
internal fun Double.r2(): Double =
    if (isNaN() || isInfinite()) 0.0 else java.math.BigDecimal.valueOf(this).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()

object PlanillaPeruCalculator {

    /**
     * Versión sin horario: las horas extras son lo que excede la jornada base diaria (8 h).
     * @param horasPorDia horas efectivas de cada día trabajado en el periodo.
     */
    fun calcular(
        config: ConfigPago,
        horasPorDia: List<Double>,
        p: ParametrosLegales = ParametrosLegales(),
        periodo: PeriodoPlanilla = PeriodoPlanilla()
    ): Boleta = calcularDias(config, horasPorDia.map {
        val extra = JornadaCalculator.horasExtraDelDia(it, p.jornadaDiariaBase)
        HorasDia(it - extra, extra)
    }, p, periodo)

    /** @param dias horas ordinarias y extras de cada día (las extras ya vienen del horario). */
    fun calcularDias(
        config: ConfigPago,
        dias: List<HorasDia>,
        p: ParametrosLegales = ParametrosLegales(),
        periodo: PeriodoPlanilla = PeriodoPlanilla()
    ): Boleta = when (config.modo) {
        ModoPago.SIMPLE -> modoSimple(config, dias)
        ModoPago.AVANZADO -> modoAvanzado(config, dias, p, periodo)
    }

    /**
     * Costo de un día para el panel en vivo.
     * - Simple: horas × tarifa.
     * - Avanzado mensual: 1/30 del costo laboral mensual (sueldo, asignación, EsSalud y provisiones) + horas extras con su EsSalud.
     * - Avanzado por hora: horas del día × tarifa + extras con recargo, más EsSalud y provisiones sobre ese monto.
     *   Un día sin horas no suma costo.
     */
    fun costoDiario(config: ConfigPago, dia: HorasDia, p: ParametrosLegales = ParametrosLegales()): Double {
        if (dia.ordinarias + dia.extra <= 0.0) return 0.0
        if (config.modo == ModoPago.SIMPLE) return ((dia.ordinarias + dia.extra) * config.tarifaPorHora).r2()
        if (config.pagoPorHora)
            return modoAvanzado(config.copy(asignacionFamiliar = false), listOf(dia), p, PeriodoPlanilla(), minimoEssalud = false).costoEmpleador
        val mes = modoAvanzado(config, emptyList(), p, PeriodoPlanilla())
        val (h1, h2) = tramosExtra(listOf(dia))
        val vh = valorHoraExtra(config, p)
        val extras = h1 * vh * 1.25 + h2 * vh * 1.35
        val essalud = if (config.regimen == RegimenLaboral.MICROEMPRESA) 0.0 else p.tasaEssalud
        return (mes.costoEmpleador / p.diasMes + extras * (1 + essalud)).r2()
    }

    /** Por día: las 2 primeras horas extra van al primer tramo y el resto al segundo. */
    private fun tramosExtra(dias: List<HorasDia>): Pair<Double, Double> =
        dias.sumOf { it.extra.coerceAtMost(2.0) } to dias.sumOf { (it.extra - 2.0).coerceAtLeast(0.0) }

    private fun asignacionFamiliar(config: ConfigPago, p: ParametrosLegales) =
        if (config.asignacionFamiliar) (p.rmv * 0.10).r2() else 0.0

    /**
     * Valor hora para el sobretiempo (DS 007-2002-TR, art. 10): remuneración ordinaria ÷ 30 ÷ 8.
     * Sueldo mensual: (sueldo + asignación familiar) ÷ 240. Por hora: tarifa + asignación familiar ÷ 240.
     */
    private fun valorHoraExtra(config: ConfigPago, p: ParametrosLegales) =
        (if (config.pagoPorHora) config.tarifaPorHora else config.sueldoBase / p.horasMensualesBase) +
            asignacionFamiliar(config, p) / p.horasMensualesBase

    /**
     * Modo Simple: horas × tarifa, sin descuentos. Las horas extras se pagan a la misma tarifa, sin recargo
     * (los recargos legales del 25 % / 35 % solo se aplican en el Modo Avanzado Perú).
     */
    private fun modoSimple(config: ConfigPago, dias: List<HorasDia>): Boleta {
        val horas = dias.sumOf { it.ordinarias }
        val (he1, he2) = tramosExtra(dias)
        val extras = he1 + he2
        val t = config.tarifaPorHora
        val montoExtras = (extras * t).r2()
        return Boleta(
            modo = ModoPago.SIMPLE,
            horasOrdinarias = horas,
            horasExtra25 = he1,
            horasExtra35 = he2,
            ingresos = buildList {
                add(LineaBoleta("Horas trabajadas (${"%.2f".format(horas)} h × ${"%.2f".format(t)})", (horas * t).r2()))
                if (montoExtras > 0) add(LineaBoleta("Horas extras sin recargo (${"%.2f".format(extras)} h × ${"%.2f".format(t)})", montoExtras))
            },
            descuentos = emptyList(),
            aportesEmpleador = emptyList(),
            provisiones = emptyList()
        )
    }

    /**
     * Modo Avanzado Perú.
     * - Sueldo mensual: cubre los 30 días (incluido el descanso semanal); se paga completo salvo faltas injustificadas
     *   (1/30 cada una), tardanzas no justificadas o ingreso a mitad de mes.
     * - Remuneración por hora: se paga cada hora ordinaria trabajada × tarifa (las horas no trabajadas no se pagan, así que
     *   no hay descuento por faltas ni tardanzas). Gratificación, CTS y vacaciones se provisionan sobre lo ganado en el mes;
     *   con menos de 4 horas diarias en promedio (tiempo parcial) no corresponden CTS ni vacaciones.
     * @param minimoEssalud aplica la base mínima de EsSalud (RMV); se omite al costear un solo día.
     */
    private fun modoAvanzado(
        config: ConfigPago,
        dias: List<HorasDia>,
        p: ParametrosLegales,
        periodo: PeriodoPlanilla,
        minimoEssalud: Boolean = true
    ): Boleta {
        val porHora = config.pagoPorHora
        val sueldo = config.sueldoBase
        val tarifa = config.tarifaPorHora
        val af = asignacionFamiliar(config, p)
        val diasComputables = periodo.diasComputables.coerceIn(0, p.diasMes)
        val diasConHoras = dias.count { it.ordinarias + it.extra > 0 }
        val faltas = if (porHora) 0 else periodo.faltas.coerceIn(0, diasComputables)
        val diasPagados = if (porHora) diasConHoras else diasComputables - faltas
        val esMicro = config.regimen == RegimenLaboral.MICROEMPRESA
        val ordinarias = dias.sumOf { it.ordinarias }
        val promedioDiario = if (diasConHoras > 0) dias.sumOf { it.ordinarias + it.extra } / diasConHoras else 0.0
        val tiempoParcial = porHora && diasConHoras > 0 && promedioDiario < 4.0

        // --- Ingresos ---
        val remBasica = if (porHora) (ordinarias * tarifa).r2() else (sueldo / p.diasMes * diasPagados).r2()
        val vhExtra = valorHoraExtra(config, p)
        val (he25, he35) = tramosExtra(dias)
        val montoHe25 = (he25 * vhExtra * 1.25).r2()
        val montoHe35 = (he35 * vhExtra * 1.35).r2()
        val descTardanza = if (porHora) 0.0 else (periodo.minutosTardanza / 60.0 * sueldo / p.horasMensualesBase).r2()
        val ingresos = buildList {
            if (porHora) {
                add(LineaBoleta("Remuneración por horas (${"%.2f".format(ordinarias)} h × ${"%.2f".format(tarifa)})", remBasica))
            } else {
                val detalle = buildString {
                    append("$diasPagados de ${p.diasMes} días")
                    if (faltas > 0) append(" · $faltas ${if (faltas == 1) "falta" else "faltas"}")
                    if (diasComputables < p.diasMes) append(" · ingreso en el mes")
                }
                add(LineaBoleta("Remuneración básica ($detalle)", remBasica))
            }
            if (af > 0) add(LineaBoleta("Asignación familiar (10 % RMV)", af))
            if (montoHe25 > 0) add(LineaBoleta("Horas extras 25 % (${"%.2f".format(he25)} h)", montoHe25))
            if (montoHe35 > 0) add(LineaBoleta("Horas extras 35 % (${"%.2f".format(he35)} h)", montoHe35))
            if (descTardanza > 0) add(LineaBoleta("(−) Tardanzas no justificadas (${periodo.minutosTardanza} min)", -descTardanza))
        }
        val bruto = ingresos.sumOf { it.monto }.coerceAtLeast(0.0)

        // --- Descuentos al trabajador (sobre la remuneración del mes) ---
        val descuentos = mutableListOf<LineaBoleta>()
        if (config.sistemaPension == SistemaPension.ONP) {
            descuentos += LineaBoleta("ONP (13 %)", (bruto * p.tasaOnp).r2())
        } else {
            val afp = config.sistemaPension.etiqueta
            descuentos += LineaBoleta("$afp — Aporte obligatorio (10 %)", (bruto * p.aporteAfp).r2())
            descuentos += LineaBoleta(
                "$afp — Prima de seguro (${pct(p.primaSeguroAfp)})",
                (minOf(bruto, p.remuneracionMaximaAsegurable) * p.primaSeguroAfp).r2()
            )
            if (!config.comisionMixta) {
                val c = p.comisionFlujo[config.sistemaPension] ?: 0.0
                descuentos += LineaBoleta("$afp — Comisión sobre flujo (${pct(c)})", (bruto * c).r2())
            }
        }

        // --- Beneficios sociales según régimen (por hora: sobre lo ganado en el mes) ---
        val remComputable = if (porHora) remBasica + af else sueldo + af
        val factorGratiCts = when (config.regimen) {
            RegimenLaboral.GENERAL -> 1.0
            RegimenLaboral.PEQUENA_EMPRESA -> 0.5 // media gratificación y 15 remuneraciones diarias de CTS por año
            RegimenLaboral.MICROEMPRESA -> 0.0
        }
        val mesCompleto = diasComputables == p.diasMes
        // Por hora, la remuneración computable ya es lo ganado: no se vuelve a prorratear por días
        val fraccionDias = if (porHora) 1.0 else diasPagados.toDouble() / p.diasMes
        // Gratificación (Ley 27735): un sueldo en julio y otro en diciembre → 1/6 por mes calendario completo
        val gratiMensual = if (mesCompleto || porHora) (remComputable * factorGratiCts / 6.0).r2() else 0.0
        val bonifExtra = (gratiMensual * p.bonificacionExtraordinaria).r2()
        // CTS (DS 001-97-TR): (remuneración + 1/6 de la gratificación) ÷ 12 por mes; las faltas descuentan 1/30.
        // Tiempo parcial (menos de 4 h diarias): sin CTS ni vacaciones (DS 001-97-TR art. 4; DL 713 art. 12)
        val ctsMensual = if (tiempoParcial) 0.0 else (factorGratiCts * (remComputable + remComputable * factorGratiCts / 6.0) / 12.0 * fraccionDias).r2()
        // Vacaciones: 30 días al año en el Régimen General, 15 en el REMYPE
        val vacacionesMensual = if (tiempoParcial) 0.0 else (remComputable / (if (config.regimen == RegimenLaboral.GENERAL) 12.0 else 24.0) * fraccionDias).r2()

        // --- Renta de 5.ª categoría: proyección anual (remuneración fija × 12 + gratificaciones + bonificación + extras del mes) ---
        val extrasMes = montoHe25 + montoHe35
        val gratiAnual = remComputable * factorGratiCts * 2 // julio + diciembre
        val proyeccionAnual = remComputable * 12 + gratiAnual * (1 + p.bonificacionExtraordinaria) + extrasMes
        val quintaMensual = (impuestoQuintaAnual(proyeccionAnual, p) / 12.0).r2()
        if (quintaMensual > 0) descuentos += LineaBoleta("Renta de 5.ª categoría (retención proyectada)", quintaMensual)

        // --- Aportes del empleador ---
        val aportes = if (esMicro) {
            listOf(LineaBoleta("SIS Microempresas (aporte según afiliación; no calculado)", 0.0))
        } else {
            // Base mínima de EsSalud: la RMV (proporcional si ingresó a mitad de mes); por hora, solo si hubo horas en el mes
            val minimo = if (!minimoEssalud || (porHora && bruto <= 0.0)) 0.0 else p.rmv * diasComputables / p.diasMes
            val baseEssalud = maxOf(bruto, minimo)
            listOf(LineaBoleta("EsSalud (9 %)", (baseEssalud * p.tasaEssalud).r2()))
        }

        val provisiones = buildList {
            if (gratiMensual > 0) {
                add(LineaBoleta("Provisión gratificación (julio y diciembre)", gratiMensual))
                add(LineaBoleta("Provisión bonificación extraordinaria (9 %)", bonifExtra))
            }
            if (ctsMensual > 0) add(LineaBoleta("Provisión CTS", ctsMensual))
            if (vacacionesMensual > 0) add(LineaBoleta("Provisión vacaciones (${if (config.regimen == RegimenLaboral.GENERAL) 30 else 15} días al año)", vacacionesMensual))
        }

        val advertencias = buildList {
            val rmvHora = p.rmv / p.horasMensualesBase
            if (porHora) {
                if (tarifa <= 0) add("Falta registrar la tarifa por hora del colaborador.")
                else if (tarifa < rmvHora - 0.005) add("La tarifa por hora (S/ ${"%.2f".format(tarifa)}) es menor que la RMV por hora (S/ ${"%.2f".format(rmvHora)} = RMV ÷ 240).")
                if (tiempoParcial) add("Promedio de ${"%.1f".format(promedioDiario)} h por día: tiempo parcial (menos de 4 h diarias), sin CTS ni vacaciones. El contrato a tiempo parcial debe registrarse ante la AAT.")
                if (diasConHoras > 0 && !esMicro) add("EsSalud se calcula sobre una base mínima de una RMV aunque las horas del mes sumen menos.")
            } else if (sueldo <= 0) add("Falta registrar el sueldo base del colaborador.")
            else if (sueldo < p.rmv) add("El sueldo base (S/ ${"%,.2f".format(sueldo)}) es menor que la RMV vigente (S/ ${"%,.2f".format(p.rmv)}). Solo es válido si la jornada es menor a 4 horas diarias.")
            if (config.asignacionFamiliar && config.regimen != RegimenLaboral.GENERAL)
                add("En el REMYPE la asignación familiar no es obligatoria; se está pagando por acuerdo.")
            if (esMicro) add("Microempresa: sin CTS ni gratificaciones; el seguro de salud es el SIS (o EsSalud si el empleador lo eligió).")
            if (quintaMensual > 0) add("La retención de 5.ª categoría es una proyección (÷ 12). En la planilla real se aplica el procedimiento del art. 40 del Reglamento de la LIR.")
        }

        return Boleta(
            ModoPago.AVANZADO, ordinarias, he25, he35, ingresos, descuentos, aportes, provisiones,
            diasLaborados = diasPagados, faltas = faltas, advertencias = advertencias
        )
    }

    private fun pct(v: Double) = "%.2f %%".format(v * 100).replace(".00 %", " %")

    /** Impuesto anual de 5ta categoría con tramos progresivos sobre la renta neta (bruto − 7 UIT). */
    fun impuestoQuintaAnual(rentaBrutaAnual: Double, p: ParametrosLegales = ParametrosLegales()): Double {
        var rentaNeta = rentaBrutaAnual - p.deduccionQuintaUit * p.uit
        if (rentaNeta <= 0) return 0.0
        var impuesto = 0.0
        var limiteAnterior = 0.0
        for ((limiteUit, tasa) in p.tramosQuinta) {
            val limite = if (limiteUit == Double.MAX_VALUE) Double.MAX_VALUE else limiteUit * p.uit
            val tramo = minOf(rentaNeta, limite - limiteAnterior)
            if (tramo <= 0) break
            impuesto += tramo * tasa
            rentaNeta -= tramo
            limiteAnterior = limite
        }
        return impuesto.r2()
    }
}
