package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.SistemaPension
import kotlin.math.round

/**
 * Parámetros legales vigentes. Se exponen como datos para poder actualizarlos
 * (p. ej. vía Remote Config) cuando cambien la RMV, la UIT o las comisiones AFP.
 */
data class ParametrosLegales(
    val rmv: Double = 1130.0,              // Remuneración Mínima Vital (DS 006-2024-TR)
    val uit: Double = 5350.0,              // UIT de referencia — verificar valor del año en curso
    val horasMensualesBase: Double = 240.0, // 30 días × 8 h
    val jornadaDiariaBase: Double = 8.0,
    val tasaOnp: Double = 0.13,
    val aporteAfp: Double = 0.10,
    val primaSeguroAfp: Double = 0.017,
    val comisionFlujo: Map<SistemaPension, Double> = mapOf(
        SistemaPension.AFP_INTEGRA to 0.0155,
        SistemaPension.AFP_PRIMA to 0.0160,
        SistemaPension.AFP_PROFUTURO to 0.0169,
        SistemaPension.AFP_HABITAT to 0.0147
    ),
    val tasaEssalud: Double = 0.09,
    val bonificacionExtraordinaria: Double = 0.09,
    val tramosQuinta: List<Pair<Double, Double>> = listOf( // (límite superior en UIT, tasa)
        5.0 to 0.08, 20.0 to 0.14, 35.0 to 0.17, 45.0 to 0.20, Double.MAX_VALUE to 0.30
    ),
    val deduccionQuintaUit: Double = 7.0
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
    val provisiones: List<LineaBoleta>
) {
    val totalIngresos get() = ingresos.sumOf { it.monto }.r2()
    val totalDescuentos get() = descuentos.sumOf { it.monto }.r2()
    val netoAPagar get() = (totalIngresos - totalDescuentos).r2()
    val costoEmpleador get() =
        (totalIngresos + aportesEmpleador.sumOf { it.monto } + provisiones.sumOf { it.monto }).r2()
}

internal fun Double.r2(): Double = round(this * 100) / 100

object PlanillaPeruCalculator {

    /**
     * Versión sin horario: las horas extras son lo que excede la jornada base diaria (8 h).
     * @param horasPorDia horas efectivas de cada día trabajado en el periodo.
     */
    fun calcular(
        config: ConfigPago,
        horasPorDia: List<Double>,
        p: ParametrosLegales = ParametrosLegales()
    ): Boleta = calcularDias(config, horasPorDia.map {
        val extra = JornadaCalculator.horasExtraDelDia(it, p.jornadaDiariaBase)
        HorasDia(it - extra, extra)
    }, p)

    /** @param dias horas ordinarias y extras de cada día (las extras ya vienen del horario). */
    fun calcularDias(
        config: ConfigPago,
        dias: List<HorasDia>,
        p: ParametrosLegales = ParametrosLegales()
    ): Boleta = when (config.modo) {
        ModoPago.SIMPLE -> modoSimple(config, dias)
        ModoPago.AVANZADO -> modoAvanzado(config, dias, p)
    }

    /** Por día: las 2 primeras horas extra van al primer tramo y el resto al segundo. */
    private fun tramosExtra(dias: List<HorasDia>): Pair<Double, Double> =
        dias.sumOf { it.extra.coerceAtMost(2.0) } to dias.sumOf { (it.extra - 2.0).coerceAtLeast(0.0) }

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

    private fun modoAvanzado(config: ConfigPago, dias: List<HorasDia>, p: ParametrosLegales): Boleta {
        val valorHora = config.sueldoBase / p.horasMensualesBase

        // Horas extras por día: 25 % las 2 primeras, 35 % a partir de la tercera
        val (he25, he35) = tramosExtra(dias)
        val ordinarias = dias.sumOf { it.ordinarias }.coerceAtMost(p.horasMensualesBase)

        val remuneracionOrdinaria = (ordinarias * valorHora).r2()
        val asignacionFamiliar = if (config.asignacionFamiliar) (p.rmv * 0.10).r2() else 0.0
        val montoHe25 = (he25 * valorHora * 1.25).r2()
        val montoHe35 = (he35 * valorHora * 1.35).r2()

        val ingresos = buildList {
            add(LineaBoleta("Remuneración ordinaria (${"%.2f".format(ordinarias)} h)", remuneracionOrdinaria))
            if (asignacionFamiliar > 0) add(LineaBoleta("Asignación familiar (10 % RMV)", asignacionFamiliar))
            if (montoHe25 > 0) add(LineaBoleta("Horas extras 25 % (${"%.2f".format(he25)} h)", montoHe25))
            if (montoHe35 > 0) add(LineaBoleta("Horas extras 35 % (${"%.2f".format(he35)} h)", montoHe35))
        }
        val bruto = ingresos.sumOf { it.monto }

        // --- Descuentos al trabajador ---
        val descuentos = mutableListOf<LineaBoleta>()
        if (config.sistemaPension == SistemaPension.ONP) {
            descuentos += LineaBoleta("ONP (13 %)", (bruto * p.tasaOnp).r2())
        } else {
            descuentos += LineaBoleta("${config.sistemaPension.etiqueta} — Aporte obligatorio (10 %)", (bruto * p.aporteAfp).r2())
            descuentos += LineaBoleta("${config.sistemaPension.etiqueta} — Prima de seguro", (bruto * p.primaSeguroAfp).r2())
            if (!config.comisionMixta) {
                val c = p.comisionFlujo[config.sistemaPension] ?: 0.0
                descuentos += LineaBoleta("${config.sistemaPension.etiqueta} — Comisión flujo", (bruto * c).r2())
            }
        }

        // --- Beneficios según régimen ---
        val remComputable = config.sueldoBase + asignacionFamiliar
        val factorBeneficios = when (config.regimen) {
            RegimenLaboral.GENERAL -> 1.0
            RegimenLaboral.PEQUENA_EMPRESA -> 0.5
            RegimenLaboral.MICROEMPRESA -> 0.0
        }
        val gratiMensual = (remComputable / 6.0 * factorBeneficios).r2()
        val bonifExtra = (gratiMensual * p.bonificacionExtraordinaria).r2()
        val ctsMensual = ((remComputable + remComputable / 6.0) / 12.0 * factorBeneficios).r2()

        // --- Quinta categoría: proyección anual ---
        val proyeccionAnual = bruto * 12 + (gratiMensual + bonifExtra) * 12
        val quintaMensual = (impuestoQuintaAnual(proyeccionAnual, p) / 12.0).r2()
        if (quintaMensual > 0) descuentos += LineaBoleta("Renta de 5ta categoría (proyección)", quintaMensual)

        // --- Aportes del empleador ---
        val aportes = if (config.regimen == RegimenLaboral.MICROEMPRESA) {
            listOf(LineaBoleta("SIS — Seguro Integral de Salud (Microempresa)", 0.0))
        } else {
            listOf(LineaBoleta("EsSalud (9 %)", (maxOf(bruto, p.rmv) * p.tasaEssalud).r2()))
        }

        val provisiones = buildList {
            if (factorBeneficios > 0) {
                add(LineaBoleta("Provisión CTS", ctsMensual))
                add(LineaBoleta("Provisión gratificación (Jul/Dic)", gratiMensual))
                add(LineaBoleta("Bonificación extraordinaria (9 %)", bonifExtra))
            }
        }

        return Boleta(ModoPago.AVANZADO, ordinarias, he25, he35, ingresos, descuentos, aportes, provisiones)
    }

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
