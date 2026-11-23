package com.juanreyna.registrotiempo.domain.calc

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
    val boleta: Boleta
) {
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

    fun construir(
        colaboradores: List<Usuario>,
        marcajesMes: List<Marcaje>,
        ahoraMillis: Long,
        parametros: ParametrosLegales = ParametrosLegales()
    ): List<ResumenColaborador> {
        val porColaborador = marcajesMes.groupBy { it.colaboradorId }
        return colaboradores.map { u ->
            val dias = diasDe(porColaborador[u.uid].orEmpty(), ahoraMillis, u.horario)
            ResumenColaborador(
                usuario = u,
                dias = dias,
                boleta = PlanillaPeruCalculator.calcularDias(u.configPago, dias.map { it.horas }, parametros)
            )
        }
    }
}
