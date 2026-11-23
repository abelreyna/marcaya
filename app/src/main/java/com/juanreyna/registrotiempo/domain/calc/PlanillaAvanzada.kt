package com.juanreyna.registrotiempo.domain.calc

import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.Usuario

/**
 * Una fila de la planilla mensual del Modo Avanzado Perú, con cada concepto en su columna
 * (los montos salen de la boleta que calcula [PlanillaPeruCalculator]).
 */
data class FilaPlanilla(
    val numero: Int,
    val usuario: Usuario,
    val boleta: Boleta,
    val documento: String,
    val nombre: String,
    val porHora: Boolean,
    val diasComputables: Int,
    val diasTrabajados: Int,
    val basica: Double,
    val asignacionFamiliar: Double,
    val extras25: Double,
    val extras35: Double,
    val tardanzas: Double,
    val onp: Double,
    val afpFondo: Double,
    val afpPrima: Double,
    val afpComision: Double,
    val quinta: Double,
    val essalud: Double,
    val gratificacion: Double,
    val bonificacion: Double,
    val cts: Double,
    val vacaciones: Double
) {
    val ingresos get() = boleta.totalIngresos
    val descuentos get() = boleta.totalDescuentos
    val neto get() = boleta.netoAPagar
    val aportes get() = boleta.aportesEmpleador.sumOf { it.monto }.r2()
    val provisiones get() = (gratificacion + bonificacion + cts + vacaciones).r2()
    val costo get() = boleta.costoEmpleador
}

object PlanillaAvanzada {

    fun etiquetaRegimen(r: RegimenLaboral) = when (r) {
        RegimenLaboral.GENERAL -> "General"
        RegimenLaboral.PEQUENA_EMPRESA -> "Pequeña Empresa"
        RegimenLaboral.MICROEMPRESA -> "Microempresa"
    }

    /** Apellidos y nombres como en el T-Registro ("Paterno Materno, Nombres"); si no se registraron, el nombre de la cuenta. */
    fun nombreLegal(u: Usuario): String {
        val c = u.configPago
        val apellidos = listOf(c.apellidoPaterno, c.apellidoMaterno).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
        return if (apellidos.isNotEmpty() && c.nombres.isNotBlank()) "$apellidos, ${c.nombres.trim()}" else u.nombre
    }

    /** Filas de los colaboradores en Modo Avanzado, ordenadas por apellidos y nombres. */
    fun filas(resumenes: List<ResumenColaborador>): List<FilaPlanilla> =
        resumenes.filter { it.usuario.configPago.modo == ModoPago.AVANZADO }
            .sortedBy { nombreLegal(it.usuario).lowercase() }
            .mapIndexed { i, r -> fila(i + 1, r) }

    fun fila(numero: Int, r: ResumenColaborador): FilaPlanilla {
        val b = r.boleta
        val porHora = r.usuario.configPago.pagoPorHora
        fun linea(texto: String) = (b.ingresos + b.descuentos + b.aportesEmpleador + b.provisiones)
            .firstOrNull { it.concepto.contains(texto) }?.monto ?: 0.0
        return FilaPlanilla(
            numero = numero,
            usuario = r.usuario,
            boleta = b,
            documento = r.usuario.dni.ifBlank { "—" },
            nombre = nombreLegal(r.usuario),
            porHora = porHora,
            diasComputables = b.diasComputables,
            diasTrabajados = if (porHora) b.diasLaborados ?: 0 else r.dias.count { it.minutosEfectivos > 0 },
            basica = linea("Remuneración"),
            asignacionFamiliar = linea("Asignación familiar"),
            extras25 = linea("Horas extras 25"),
            extras35 = linea("Horas extras 35"),
            tardanzas = linea("(−) Tardanzas"),
            onp = linea("ONP"),
            afpFondo = linea("Aporte obligatorio"),
            afpPrima = linea("Prima de seguro"),
            afpComision = linea("Comisión sobre flujo"),
            quinta = linea("Renta de 5.ª"),
            essalud = linea("EsSalud"),
            gratificacion = linea("Provisión gratificación"),
            bonificacion = linea("Provisión bonificación"),
            cts = linea("Provisión CTS"),
            vacaciones = linea("Provisión vacaciones")
        )
    }
}
