package com.juanreyna.registrotiempo.domain.calc

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
    val fueraRango: Boolean,
    val validacionFacial: Boolean
)

data class ResumenColaborador(
    val usuario: Usuario,
    val dias: List<DiaTrabajado>,
    val boleta: Boleta
) {
    val horasTotales get() = dias.sumOf { it.minutosEfectivos } / 60.0
    val tardanzas get() = dias.count { it.tardanza }
}

object ResumenMensual {

    fun diasDe(marcajes: List<Marcaje>, ahoraMillis: Long, horaIngresoEsperada: String = "08:00"): List<DiaTrabajado> =
        marcajes.groupBy { it.fecha }.toSortedMap().map { (fecha, delDia) ->
            val r = JornadaCalculator.resumir(delDia, ahoraMillis)
            val ingreso = delDia.firstOrNull { it.tipo == TipoMarcaje.INGRESO }?.timestampMillis
            DiaTrabajado(
                fecha = fecha,
                ingreso = ingreso,
                salida = delDia.lastOrNull { it.tipo == TipoMarcaje.SALIDA }?.timestampMillis,
                minutosEfectivos = r.minutosEfectivos,
                minutosDescanso = r.minutosDescanso,
                tardanza = Fechas.esTardanza(ingreso, horaIngresoEsperada),
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
            val dias = diasDe(porColaborador[u.uid].orEmpty(), ahoraMillis, u.horaIngresoEsperada)
            ResumenColaborador(
                usuario = u,
                dias = dias,
                boleta = PlanillaPeruCalculator.calcular(u.configPago, dias.map { it.minutosEfectivos / 60.0 }, parametros)
            )
        }
    }
}
