package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.EstadoIncidencia
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.Incidencia
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.Fechas

/**
 * Detecta las incidencias del mes de un colaborador a partir de sus marcajes y su horario,
 * y les asigna el estado según la justificación enviada (por justificar, en revisión,
 * justificada o no autorizada).
 */
object DetectorIncidencias {

    /** Minutos después de la salida programada sin marcar salida para considerarla olvidada. */
    private const val GRACIA_SIN_SALIDA = 120

    fun detectar(
        u: Usuario,
        marcajesMes: List<Marcaje>,
        justificaciones: List<Justificacion>,
        mes: String,
        ahoraMillis: Long
    ): List<Incidencia> {
        if (!u.activo) return emptyList()
        val h = u.horario
        val hoy = Fechas.fechaDe(ahoraMillis)
        val minutoAhora = Fechas.minutosDelDia(ahoraMillis)
        val desde = u.creadoMillis?.let { Fechas.fechaDe(it) } ?: ""
        val porFecha = marcajesMes.filter { it.colaboradorId == u.uid }.groupBy { it.fecha }
        val salidaProg = Horario.minutos(h.salida)
        val cruza = salidaProg <= Horario.minutos(h.entrada)
        val crudas = mutableListOf<Triple<String, TipoIncidencia, Pair<Long, String>>>()

        for (fecha in Fechas.diasDelMes(mes)) {
            if (fecha > hoy) break
            if (fecha < desde) continue
            val delDia = porFecha[fecha].orEmpty()
            val esHoy = fecha == hoy
            if (delDia.isEmpty()) {
                // Falta: día laborable sin ningún marcaje, una vez terminada la jornada programada
                val jornadaTerminada = !esHoy || (!cruza && minutoAhora > salidaProg)
                if (h.trabajaEl(Fechas.diaSemanaIso(fecha)) && jornadaTerminada)
                    crudas += Triple(fecha, TipoIncidencia.FALTA, 0L to "No registró asistencia (horario ${h.entrada}–${h.salida})")
                continue
            }
            val d = ResumenMensual.diaDe(fecha, delDia, ahoraMillis, h)
            if (d.minutosTarde > 0)
                crudas += Triple(fecha, TipoIncidencia.TARDANZA, d.minutosTarde to "Ingresó a las ${Fechas.hhmmOpt(d.ingreso)} · ${d.minutosTarde} min tarde (entrada ${h.entrada})")
            if (d.fueraRango)
                crudas += Triple(fecha, TipoIncidencia.FUERA_GEOCERCA, 0L to "Marcaje registrado fuera de la zona de trabajo")
            if (d.minutosExcesoDescanso > 0)
                crudas += Triple(fecha, TipoIncidencia.EXCESO_DESCANSO, d.minutosExcesoDescanso to "Descanso excedido en ${d.minutosExcesoDescanso} min (programado ${Fechas.duracion(h.minutosDescansoProgramado.toLong())})")
            if (d.minutosSalidaAnticipada > 0)
                crudas += Triple(fecha, TipoIncidencia.SALIDA_ANTICIPADA, d.minutosSalidaAnticipada to "Salió a las ${Fechas.hhmmOpt(d.salida)} · ${d.minutosSalidaAnticipada} min antes (salida ${h.salida})")
            val sinSalida = d.salida == null && d.ingreso != null &&
                (!esHoy || (!cruza && minutoAhora > salidaProg + GRACIA_SIN_SALIDA))
            if (sinSalida)
                crudas += Triple(fecha, TipoIncidencia.SIN_SALIDA, 0L to "No marcó salida (salida programada ${h.salida})")
        }

        return crudas.map { (fecha, tipo, datos) ->
            val j = justificacionDe(justificaciones, u.uid, fecha, tipo)
            val estado = when (j?.estado) {
                null -> EstadoIncidencia.POR_JUSTIFICAR
                EstadoJustificacion.PENDIENTE -> EstadoIncidencia.EN_REVISION
                EstadoJustificacion.APROBADA -> EstadoIncidencia.JUSTIFICADA
                EstadoJustificacion.RECHAZADA -> EstadoIncidencia.NO_AUTORIZADA
            }
            Incidencia(u.uid, u.nombre, fecha, tipo, datos.first, datos.second, estado, j)
        }.sortedWith(compareByDescending<Incidencia> { it.fecha }.thenBy { it.tipo.ordinal })
    }

    /**
     * Justificación más reciente para esa incidencia. Las justificaciones antiguas (sin tipo de incidencia)
     * cubren cualquier incidencia de su fecha.
     */
    fun justificacionDe(js: List<Justificacion>, uid: String, fecha: String, tipo: TipoIncidencia): Justificacion? =
        js.filter { it.colaboradorId == uid && it.fecha == fecha && (it.incidencia == tipo || it.incidencia == null) }
            .sortedWith(compareBy<Justificacion> { if (it.incidencia == tipo) 0 else 1 }.thenByDescending { it.creadoMillis ?: Long.MAX_VALUE })
            .firstOrNull()
}
