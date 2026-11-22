package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.EstadoColaborador
import com.juanreyna.registrotiempo.domain.model.Horario
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

/** Incidencias de un día frente al horario programado (minutos ya pasada la tolerancia). */
data class IncidenciasDia(
    val minutosTarde: Long = 0,
    val minutosExcesoDescanso: Long = 0,
    val minutosExtra: Long = 0
) {
    val tardanza get() = minutosTarde > 0
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

    fun estado(marcajesDelDia: List<Marcaje>, horario: Horario): EstadoColaborador {
        val ultimo = marcajesDelDia.filter { it.timestampMillis != null }
            .maxByOrNull { it.timestampMillis!! } ?: return EstadoColaborador.SIN_MARCAR
        val ingreso = marcajesDelDia.firstOrNull { it.tipo == TipoMarcaje.INGRESO }
        return when {
            ultimo.tipo == TipoMarcaje.SALIDA -> EstadoColaborador.JORNADA_CERRADA
            ultimo.tipo == TipoMarcaje.INICIO_DESCANSO -> EstadoColaborador.EN_DESCANSO
            !ultimo.dentroGeocerca -> EstadoColaborador.FUERA_RANGO
            Fechas.esTardanza(ingreso?.timestampMillis, horario.entrada, horario.toleranciaMin) -> EstadoColaborador.TARDANZA
            else -> EstadoColaborador.DENTRO_AREA
        }
    }

    /**
     * Compara la jornada con el horario:
     * - Tardanza: ingreso después de la entrada + tolerancia (se reportan los minutos desde la entrada).
     * - Exceso de descanso: descanso tomado − descanso programado, si supera la tolerancia. Esos minutos
     *   ya no cuentan como trabajados, así que no se pagan.
     * - Horas extras: lo trabajado después de la salida programada, si supera la tolerancia
     *   (con la jornada abierta se usa [ahoraMillis] como salida provisional).
     */
    fun evaluar(marcajesDelDia: List<Marcaje>, resumen: ResumenJornada, horario: Horario, ahoraMillis: Long): IncidenciasDia {
        val tol = horario.toleranciaMin.coerceAtLeast(0)
        val entradaProg = Horario.minutos(horario.entrada)
        var salidaProg = Horario.minutos(horario.salida)
        val cruzaMedianoche = salidaProg <= entradaProg
        if (cruzaMedianoche) salidaProg += 24 * 60
        // Hora del día relativa a la entrada (turnos que cruzan la medianoche)
        fun rel(millis: Long): Int {
            val m = Fechas.minutosDelDia(millis)
            return if (cruzaMedianoche && m < entradaProg - 6 * 60) m + 24 * 60 else m
        }
        val ordenados = marcajesDelDia.filter { it.timestampMillis != null }.sortedBy { it.timestampMillis }
        val ingreso = ordenados.firstOrNull { it.tipo == TipoMarcaje.INGRESO }?.timestampMillis
            ?: return IncidenciasDia()

        val tarde = (rel(ingreso) - entradaProg).toLong()
        val minutosTarde = if (tarde > tol) tarde else 0L

        val exceso = resumen.minutosDescanso - horario.minutosDescansoProgramado
        val minutosExceso = if (exceso > tol) exceso else 0L

        val fin = ordenados.lastOrNull { it.tipo == TipoMarcaje.SALIDA }?.timestampMillis
            ?: if (resumen.abierta && !resumen.enDescanso) ahoraMillis else null
        val extra = fin?.let { (rel(it) - salidaProg).toLong() } ?: 0L
        val minutosExtra = if (extra > tol) extra.coerceAtMost(resumen.minutosEfectivos) else 0L

        return IncidenciasDia(minutosTarde, minutosExceso, minutosExtra)
    }

    /** Horas extra del día: todo lo que excede la jornada base diaria (8 h). */
    fun horasExtraDelDia(horasEfectivas: Double, jornadaBase: Double = 8.0): Double =
        (horasEfectivas - jornadaBase).coerceAtLeast(0.0)
}
