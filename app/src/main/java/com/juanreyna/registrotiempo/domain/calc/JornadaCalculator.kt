package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.EstadoColaborador
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.util.Fechas

/** Resumen de una jornada diaria a partir de sus 4 eventos de marcaje. */
data class ResumenJornada(
    val minutosEfectivos: Long,
    val minutosDescanso: Long,
    val abierta: Boolean,
    val enDescanso: Boolean,
    val ultimoTipo: TipoMarcaje?
) {
    val horasEfectivas: Double get() = minutosEfectivos / 60.0
}

object JornadaCalculator {

    /**
     * Calcula los minutos efectivos: (Salida − Ingreso) − descansos.
     * Si la jornada sigue abierta se usa [ahoraMillis] (hora de referencia) como cierre provisional.
     */
    fun resumir(marcajesDelDia: List<Marcaje>, ahoraMillis: Long): ResumenJornada {
        val ordenados = marcajesDelDia.filter { it.timestampMillis != null }
            .sortedBy { it.timestampMillis }
        var inicioTramo: Long? = null
        var inicioDescanso: Long? = null
        var efectivosMs = 0L
        var descansoMs = 0L
        var abierta = false
        var enDescanso = false

        for (m in ordenados) {
            val t = m.timestampMillis!!
            when (m.tipo) {
                TipoMarcaje.INGRESO -> {
                    inicioTramo = t; abierta = true; enDescanso = false
                }
                TipoMarcaje.INICIO_DESCANSO -> {
                    inicioTramo?.let { efectivosMs += t - it }
                    inicioTramo = null; inicioDescanso = t; enDescanso = true
                }
                TipoMarcaje.FIN_DESCANSO -> {
                    inicioDescanso?.let { descansoMs += t - it }
                    inicioDescanso = null; inicioTramo = t; enDescanso = false
                }
                TipoMarcaje.SALIDA -> {
                    inicioTramo?.let { efectivosMs += t - it }
                    inicioDescanso?.let { descansoMs += t - it }
                    inicioTramo = null; inicioDescanso = null
                    abierta = false; enDescanso = false
                }
            }
        }
        if (abierta) {
            inicioTramo?.let { efectivosMs += (ahoraMillis - it).coerceAtLeast(0) }
            inicioDescanso?.let { descansoMs += (ahoraMillis - it).coerceAtLeast(0) }
        }
        return ResumenJornada(
            minutosEfectivos = efectivosMs / 60_000,
            minutosDescanso = descansoMs / 60_000,
            abierta = abierta,
            enDescanso = enDescanso,
            ultimoTipo = ordenados.lastOrNull()?.tipo
        )
    }

    /** Próximos eventos permitidos según el último marcaje del día. */
    fun siguientesPermitidos(ultimo: TipoMarcaje?): Set<TipoMarcaje> = when (ultimo) {
        null -> setOf(TipoMarcaje.INGRESO)
        TipoMarcaje.INGRESO -> setOf(TipoMarcaje.INICIO_DESCANSO, TipoMarcaje.SALIDA)
        TipoMarcaje.INICIO_DESCANSO -> setOf(TipoMarcaje.FIN_DESCANSO)
        TipoMarcaje.FIN_DESCANSO -> setOf(TipoMarcaje.INICIO_DESCANSO, TipoMarcaje.SALIDA)
        TipoMarcaje.SALIDA -> emptySet()
    }

    fun estado(marcajesDelDia: List<Marcaje>, horaIngresoEsperada: String): EstadoColaborador {
        val ultimo = marcajesDelDia.filter { it.timestampMillis != null }
            .maxByOrNull { it.timestampMillis!! } ?: return EstadoColaborador.SIN_MARCAR
        val ingreso = marcajesDelDia.firstOrNull { it.tipo == TipoMarcaje.INGRESO }
        return when {
            ultimo.tipo == TipoMarcaje.SALIDA -> EstadoColaborador.JORNADA_CERRADA
            ultimo.tipo == TipoMarcaje.INICIO_DESCANSO -> EstadoColaborador.EN_DESCANSO
            !ultimo.dentroGeocerca -> EstadoColaborador.FUERA_RANGO
            Fechas.esTardanza(ingreso?.timestampMillis, horaIngresoEsperada) -> EstadoColaborador.TARDANZA
            else -> EstadoColaborador.DENTRO_AREA
        }
    }

    /** Horas extra del día: todo lo que excede la jornada base diaria (8 h). */
    fun horasExtraDelDia(horasEfectivas: Double, jornadaBase: Double = 8.0): Double =
        (horasEfectivas - jornadaBase).coerceAtLeast(0.0)
}
