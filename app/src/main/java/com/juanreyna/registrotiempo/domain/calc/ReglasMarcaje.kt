package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.util.Fechas

/** Incidencia encontrada en un marcaje: tipo, minutos de diferencia con el horario y detalle legible. */
data class Hallazgo(val tipo: TipoIncidencia, val minutos: Long, val detalle: String)

/**
 * Compara cada marcaje con el horario del colaborador. Pasada la tolerancia (por defecto 5 min) genera una incidencia
 * que el colaborador debe justificar:
 * - Ingreso después de la entrada → Tardanza.
 * - Inicio de descanso antes o después de lo programado → Descanso fuera de horario.
 * - Fin de descanso después de lo programado (o descanso más largo de lo permitido) → Retorno tardío del descanso.
 * - Salida antes de la hora → Salida anticipada; después de la hora → Salida después del horario.
 */
object ReglasMarcaje {

    /** Diferencia con signo (−720..719) entre dos horas del día, en minutos: soporta turnos que cruzan la medianoche. */
    fun diferencia(minutoReal: Int, minutoProgramado: Int): Int = (minutoReal - minutoProgramado + 720).mod(1440) - 720

    fun evaluar(h: Horario, tipo: TipoMarcaje, millis: Long, inicioDescansoMillis: Long? = null): Hallazgo? {
        val tol = h.toleranciaMin.coerceAtLeast(0)
        val hora = Fechas.hhmm(millis)
        val dif = { prog: String -> diferencia(Fechas.minutosDelDia(millis), Horario.minutos(prog)) }
        return when (tipo) {
            TipoMarcaje.INGRESO -> dif(h.entrada).takeIf { it > tol }
                ?.let { Hallazgo(TipoIncidencia.TARDANZA, it.toLong(), "Ingresó a las $hora · $it min tarde (entrada ${h.entrada})") }

            // Si ese día no tiene descanso programado, el descanso no se compara con ninguna hora
            TipoMarcaje.INICIO_DESCANSO -> if (h.minutosDescansoProgramado == 0) null else dif(h.inicioDescanso).takeIf { kotlin.math.abs(it) > tol }?.let {
                Hallazgo(
                    TipoIncidencia.DESCANSO_FUERA_HORARIO, kotlin.math.abs(it).toLong(),
                    "Salió a descanso a las $hora · ${kotlin.math.abs(it)} min ${if (it < 0) "antes" else "después"} (descanso ${h.inicioDescanso}–${h.finDescanso})"
                )
            }

            TipoMarcaje.FIN_DESCANSO -> if (h.minutosDescansoProgramado == 0) null else {
                val tarde = dif(h.finDescanso)
                val exceso = inicioDescansoMillis?.let { ((millis - it) / 60_000).toInt() - h.minutosDescansoProgramado } ?: 0
                when {
                    tarde > tol -> Hallazgo(TipoIncidencia.EXCESO_DESCANSO, tarde.toLong(), "Regresó del descanso a las $hora · $tarde min tarde (fin ${h.finDescanso})")
                    exceso > tol -> Hallazgo(
                        TipoIncidencia.EXCESO_DESCANSO, exceso.toLong(),
                        "Descanso de ${Fechas.duracion((exceso + h.minutosDescansoProgramado).toLong())} · $exceso min más de lo permitido"
                    )
                    else -> null
                }
            }

            TipoMarcaje.SALIDA -> {
                val d = dif(h.salida)
                when {
                    d < -tol -> Hallazgo(TipoIncidencia.SALIDA_ANTICIPADA, -d.toLong(), "Salió a las $hora · ${-d} min antes (salida ${h.salida})")
                    d > tol -> Hallazgo(TipoIncidencia.SALIDA_TARDIA, d.toLong(), "Salió a las $hora · $d min después (salida ${h.salida})")
                    else -> null
                }
            }
        }
    }
}
