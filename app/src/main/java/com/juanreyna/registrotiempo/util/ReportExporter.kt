package com.juanreyna.registrotiempo.util

import com.juanreyna.registrotiempo.domain.model.SistemaPension
import com.juanreyna.registrotiempo.domain.calc.ResumenMensual
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.juanreyna.registrotiempo.domain.calc.Boleta
import com.juanreyna.registrotiempo.domain.calc.CostoColaborador
import com.juanreyna.registrotiempo.domain.calc.ReporteCostos
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.ModoPago
import java.io.File

/**
 * Exportación de reportes sin servicios de pago:
 *  - PDF (boleta proyectada / registro de asistencia SUNAFIL — DS 004-2006-TR)
 *  - CSV compatible con Excel
 *  - TXT de jornada laboral para PLAME (.jor)
 */
object ReportExporter {

    private fun carpeta(context: Context) = File(context.cacheDir, "reportes").apply { mkdirs() }

    fun compartir(context: Context, archivo: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ------------------------------------------------------------------ PDF
    private class Lienzo(val doc: PdfDocument) {
        private val ancho = 595; private val alto = 842 // A4 en puntos
        private var numero = 0
        lateinit var pagina: PdfDocument.Page
        var y = 0f
        val normal = Paint().apply { textSize = 10f; color = Color.BLACK }
        val negrita = Paint(normal).apply { typeface = Typeface.DEFAULT_BOLD }
        val titulo = Paint(negrita).apply { textSize = 16f; color = Color.rgb(0x13, 0x29, 0x4B) }
        val verde = Paint(negrita).apply { color = Color.rgb(0x0F, 0x8B, 0x6D) }

        fun nuevaPagina() {
            if (numero > 0) doc.finishPage(pagina)
            numero++
            pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
            y = 50f
        }

        fun linea(texto: String, p: Paint = normal, x: Float = 40f, salto: Float = 15f) {
            if (y > alto - 60) nuevaPagina()
            pagina.canvas.drawText(texto, x, y, p)
            y += salto
        }

        fun fila(izq: String, der: String, p: Paint = normal) {
            if (y > alto - 60) nuevaPagina()
            pagina.canvas.drawText(izq, 40f, y, p)
            val w = p.measureText(der)
            pagina.canvas.drawText(der, ancho - 40f - w, y, p)
            y += 15f
        }

        fun columnas(valores: List<String>, xs: List<Float>, p: Paint = normal) {
            if (y > alto - 60) nuevaPagina()
            valores.forEachIndexed { i, v -> pagina.canvas.drawText(v, xs[i], y, p) }
            y += 14f
        }

        fun separador() { y += 6f; pagina.canvas.drawLine(40f, y - 10f, ancho - 40f, y - 10f, normal) }

        fun terminar(archivo: File) {
            linea("")
            linea("Generado por Registro de Tiempo de Personal v1.0.0 — © 2026 Juan Abel Reyna Gonzales", normal.apply { textSize = 8f })
            doc.finishPage(pagina)
            archivo.outputStream().use { doc.writeTo(it) }
            doc.close()
        }
    }

    private fun m(moneda: String, v: Double) = "$moneda %,.2f".format(v)

    fun boletaPdf(context: Context, empresa: Empresa, resumen: ResumenColaborador, mesEtiqueta: String): File {
        val l = Lienzo(PdfDocument()).apply { nuevaPagina() }
        val mon = empresa.moneda.simbolo
        val b: Boleta = resumen.boleta
        val u = resumen.usuario
        val cfg = u.configPago
        l.linea("BOLETA DE PAGO PROYECTADA", l.titulo, salto = 22f)
        l.linea(empresa.razonSocial.ifBlank { empresa.nombreComercial }, l.negrita)
        l.linea("RUC: ${empresa.ruc.ifBlank { "— (registrar en Reportes → Datos del empleador)" }}   Periodo: $mesEtiqueta")
        l.linea("Trabajador: ${u.nombre}   ${u.tipoDocumento.etiqueta}: ${u.dni.ifBlank { "—" }}")
        if (b.modo == ModoPago.AVANZADO) {
            l.linea("Cargo: ${cfg.cargo.ifBlank { "—" }}   Fecha de ingreso: ${ResumenMensual.fechaIngreso(u)?.split("-")?.reversed()?.joinToString("/") ?: "—"}")
            l.linea("Régimen laboral: ${cfg.regimen.etiqueta}   Régimen pensionario: ${cfg.sistemaPension.etiqueta}${if (cfg.sistemaPension != SistemaPension.ONP) "   CUSPP: ${cfg.cuspp.ifBlank { "—" }}" else ""}")
            l.linea(
                if (cfg.pagoPorHora) "Remuneración: por hora (${m(mon, cfg.tarifaPorHora)} la hora)"
                else "Remuneración: sueldo mensual ${m(mon, cfg.sueldoBase)} (valor hora ${m(mon, cfg.valorHora)})"
            )
            l.linea("Días laborados: ${b.diasLaborados ?: "—"}   Faltas: ${b.faltas}   Horas ordinarias: ${"%.2f".format(b.horasOrdinarias)}   Horas extras: ${"%.2f".format(b.horasExtra25 + b.horasExtra35)}")
        } else {
            l.linea("Modalidad: Modo Simple (pago por hora)")
            l.linea("Días trabajados: ${resumen.dias.size}   Horas efectivas: ${"%.2f".format(resumen.horasTotales)}   Tardanzas: ${resumen.tardanzas}")
        }
        l.separador()
        l.linea("INGRESOS", l.negrita)
        b.ingresos.forEach { l.fila(it.concepto, m(mon, it.monto)) }
        l.fila("Total ingresos", m(mon, b.totalIngresos), l.negrita)
        if (b.descuentos.isNotEmpty()) {
            l.separador(); l.linea("DESCUENTOS AL TRABAJADOR", l.negrita)
            b.descuentos.forEach { l.fila(it.concepto, m(mon, it.monto)) }
            l.fila("Total descuentos", m(mon, b.totalDescuentos), l.negrita)
        }
        l.separador()
        l.fila("NETO A PAGAR", m(mon, b.netoAPagar), l.verde)
        if (b.aportesEmpleador.isNotEmpty() || b.provisiones.isNotEmpty()) {
            l.separador(); l.linea("APORTES DEL EMPLEADOR Y BENEFICIOS SOCIALES", l.negrita)
            (b.aportesEmpleador + b.provisiones).forEach { l.fila(it.concepto, m(mon, it.monto)) }
            l.fila("Costo laboral total del empleador", m(mon, b.costoEmpleador), l.negrita)
        }
        if (b.advertencias.isNotEmpty()) {
            l.separador(); l.linea("OBSERVACIONES", l.negrita)
            b.advertencias.forEach { l.linea("• $it", Paint(l.normal).apply { textSize = 7.5f }) }
        }
        l.linea("")
        l.linea("Documento referencial: proyección calculada con los marcajes registrados a la fecha.", l.normal)
        if (b.modo == ModoPago.AVANZADO) {
            l.linea("Para valer como boleta de pago debe emitirse con la firma del empleador (DS 001-98-TR; firma digital: DS 009-2011-TR).", Paint(l.normal).apply { textSize = 7.5f })
        }
        val f = File(carpeta(context), "boleta_${resumen.usuario.dni.ifBlank { resumen.usuario.uid.take(6) }}_$mesEtiqueta.pdf".replace(" ", "_"))
        l.terminar(f)
        return f
    }

    /** Registro de control de asistencia (DS 004-2006-TR): fecha, hora de ingreso y salida por trabajador. */
    fun asistenciaSunafilPdf(context: Context, empresa: Empresa, resumenes: List<ResumenColaborador>, mesEtiqueta: String): File {
        val l = Lienzo(PdfDocument()).apply { nuevaPagina() }
        l.linea("REGISTRO DE CONTROL DE ASISTENCIA", l.titulo, salto = 20f)
        l.linea("Conforme al D.S. N.º 004-2006-TR", l.normal)
        l.linea("Empleador: ${empresa.razonSocial.ifBlank { empresa.nombreComercial }}   RUC: ${empresa.ruc.ifBlank { "—" }}   Periodo: $mesEtiqueta", l.negrita)
        l.separador()
        val xs = listOf(40f, 120f, 200f, 280f, 360f, 440f)
        resumenes.forEach { r ->
            l.linea("${r.usuario.nombre} — ${r.usuario.tipoDocumento.etiqueta} ${r.usuario.dni}", l.negrita)
            l.columnas(listOf("Fecha", "Ingreso", "Salida", "Descanso", "Efectivo", "Verificación"), xs, l.negrita)
            r.dias.forEach { d ->
                l.columnas(
                    listOf(
                        d.fecha, Fechas.hora(d.ingreso), Fechas.hora(d.salida),
                        Fechas.duracion(d.minutosDescanso), Fechas.duracion(d.minutosEfectivos),
                        buildString {
                            append(if (d.validacionFacial) "Facial ✓" else "Facial ✗")
                            if (d.tardanza) append(" · Tard.")
                            if (d.fueraRango) append(" · F.R.")
                        }
                    ), xs
                )
            }
            l.linea("Total: ${"%.2f".format(r.horasTotales)} h   Tardanzas: ${r.tardanzas}")
            l.separador()
        }
        val f = File(carpeta(context), "asistencia_sunafil_${mesEtiqueta.replace(" ", "_")}.pdf")
        l.terminar(f)
        return f
    }

    fun planillaCsv(context: Context, empresa: Empresa, resumenes: List<ResumenColaborador>, mes: String): File {
        val f = File(carpeta(context), "planilla_$mes.csv")
        val sb = StringBuilder("﻿") // BOM para que Excel reconozca UTF-8
        sb.appendLine("Colaborador;DNI/CE;Modalidad;Días;Horas efectivas;Tardanzas;Ingresos;Descuentos;Neto;Costo empleador;Moneda")
        resumenes.forEach { r ->
            val b = r.boleta
            sb.appendLine(
                listOf(
                    r.usuario.nombre, r.usuario.dni, b.modo.name, r.dias.size, "%.2f".format(r.horasTotales),
                    r.tardanzas, "%.2f".format(b.totalIngresos), "%.2f".format(b.totalDescuentos),
                    "%.2f".format(b.netoAPagar), "%.2f".format(b.costoEmpleador), empresa.moneda.simbolo
                ).joinToString(";")
            )
        }
        f.writeText(sb.toString())
        return f
    }

    private fun hm(millis: Long?) = Fechas.hora(millis).take(5)
    private fun horas(min: Long) = "%.2f".format(min / 60.0)

    /** Reporte de costos (salarios): pago por día y por colaborador, con totales del mes. */
    fun costosPdf(context: Context, empresa: Empresa, costos: List<CostoColaborador>, mesEtiqueta: String): File {
        val l = Lienzo(PdfDocument()).apply { nuevaPagina() }
        val mon = empresa.moneda.simbolo
        l.linea("REPORTE DE COSTOS LABORALES (SALARIOS)", l.titulo, salto = 20f)
        l.linea("Empleador: ${empresa.nombreComercial}   Periodo: $mesEtiqueta", l.negrita)
        l.separador()
        l.linea("RESUMEN DEL MES", l.negrita)
        l.fila("Colaboradores con marcajes", "${costos.count { it.dias.isNotEmpty() }}")
        l.fila("Horas trabajadas (ordinarias + extras)", horas(costos.sumOf { it.minutosTrabajados }) + " h")
        l.fila("Horas extras", horas(costos.sumOf { it.minutosExtra }) + " h")
        l.fila("Pagado por días trabajados", m(mon, costos.sumOf { it.pagadoPorDias }))
        l.fila("Total bruto (boletas)", m(mon, costos.sumOf { it.boleta.totalIngresos }))
        l.fila("Neto a pagar", m(mon, costos.sumOf { it.boleta.netoAPagar }), l.negrita)
        l.fila("Costo laboral total del empleador", m(mon, costos.sumOf { it.boleta.costoEmpleador }), l.verde)
        l.separador()

        val xs = listOf(40f, 108f, 150f, 192f, 240f, 290f, 370f, 450f)
        costos.forEach { c ->
            val u = c.colaborador
            l.linea("${u.nombre} — DNI/CE ${u.dni.ifBlank { "—" }}", l.negrita)
            l.linea(
                "${if (u.configPago.modo == ModoPago.SIMPLE) "Modo Simple" else "Modo Avanzado · ${u.configPago.regimen.etiqueta}"}   " +
                    "Valor hora: ${m(mon, ReporteCostos.valorHora(u.configPago))}   Horario: ${if (u.horario.porDia) "por día" else "${u.horario.entrada}–${u.horario.salida}"}"
            )
            if (c.dias.isEmpty()) { l.linea("Sin días trabajados en el periodo."); l.separador(); return@forEach }
            l.columnas(listOf("Fecha", "Ingreso", "Salida", "H. ord.", "H. extra", "Pago ord.", "Pago extra", "Total día"), xs, l.negrita)
            c.dias.forEach { d ->
                l.columnas(
                    listOf(
                        d.fecha, hm(d.ingreso), hm(d.salida), horas(d.minutosOrdinarios), horas(d.minutosExtra),
                        "%,.2f".format(d.montoOrdinario), "%,.2f".format(d.montoExtra), "%,.2f".format(d.total)
                    ), xs
                )
            }
            l.fila("Pagado por ${c.dias.size} ${if (c.dias.size == 1) "día" else "días"} (${horas(c.minutosTrabajados)} h)", m(mon, c.pagadoPorDias), l.negrita)
            val b = c.boleta
            l.fila("Bruto del mes / Descuentos", "${m(mon, b.totalIngresos)}  /  -${m(mon, b.totalDescuentos)}")
            l.fila("Neto a pagar / Costo empleador", "${m(mon, b.netoAPagar)}  /  ${m(mon, b.costoEmpleador)}", l.negrita)
            l.separador()
        }

        val dias = ReporteCostos.porDia(costos)
        if (dias.isNotEmpty()) {
            l.linea("COSTO POR DÍA", l.negrita)
            val xd = listOf(40f, 130f, 250f, 330f)
            l.columnas(listOf("Fecha", "Colaboradores", "Horas", "Total pagado"), xd, l.negrita)
            dias.sortedBy { it.first }.forEach { (fecha, ds) ->
                l.columnas(listOf(fecha, "${ds.size}", horas(ds.sumOf { it.minutosTrabajados }), m(mon, ds.sumOf { it.total })), xd)
            }
        }
        l.linea("")
        l.linea("Montos por día = horas ordinarias × valor hora + horas extras (Simple: sin recargo; Avanzado Perú: +25 % / +35 %). Los totales del mes incluyen asignación familiar, descuentos y aportes según la boleta.", Paint(l.normal).apply { textSize = 7f })
        val f = File(carpeta(context), "costos_${mesEtiqueta.replace(" ", "_")}.pdf")
        l.terminar(f)
        return f
    }

    /** Costos en Excel (CSV): una fila por colaborador y día, más el resumen mensual por colaborador. */
    fun costosCsv(context: Context, empresa: Empresa, costos: List<CostoColaborador>, mes: String): File {
        val f = File(carpeta(context), "costos_$mes.csv")
        val mon = empresa.moneda.simbolo
        val sb = StringBuilder("﻿")
        sb.appendLine("DETALLE POR DÍA")
        sb.appendLine("Fecha;Colaborador;DNI/CE;Modalidad;Ingreso;Salida;Horas ordinarias;Horas extras;Tardanza (min);Exceso descanso (min);Valor hora;Pago ordinario;Pago extras;Total día;Moneda")
        costos.flatMap { it.dias }.sortedWith(compareBy({ it.fecha }, { it.colaborador.nombre })).forEach { d ->
            val u = d.colaborador
            sb.appendLine(
                listOf(
                    d.fecha, u.nombre, u.dni, u.configPago.modo.name, hm(d.ingreso), hm(d.salida),
                    horas(d.minutosOrdinarios), horas(d.minutosExtra), d.minutosTarde, d.minutosExcesoDescanso,
                    "%.2f".format(d.valorHora), "%.2f".format(d.montoOrdinario), "%.2f".format(d.montoExtra), "%.2f".format(d.total), mon
                ).joinToString(";")
            )
        }
        sb.appendLine()
        sb.appendLine("RESUMEN DEL MES POR COLABORADOR")
        sb.appendLine("Colaborador;DNI/CE;Días;Horas trabajadas;Horas extras;Pagado por días;Bruto mes;Descuentos;Neto a pagar;Costo empleador;Moneda")
        costos.forEach { c ->
            val b = c.boleta
            sb.appendLine(
                listOf(
                    c.colaborador.nombre, c.colaborador.dni, c.dias.size, horas(c.minutosTrabajados), horas(c.minutosExtra),
                    "%.2f".format(c.pagadoPorDias), "%.2f".format(b.totalIngresos), "%.2f".format(b.totalDescuentos),
                    "%.2f".format(b.netoAPagar), "%.2f".format(b.costoEmpleador), mon
                ).joinToString(";")
            )
        }
        f.writeText(sb.toString())
        return f
    }

    /**
     * Archivo de jornada laboral PLAME: 0601AAAAMM{RUC}.jor
     * Formato por línea: tipoDoc|numDoc|horasOrd|minOrd|horasSobretiempo|minSobretiempo|
     */
    fun plameJor(context: Context, resumenes: List<ResumenColaborador>, mes: String, ruc: String): File {
        val f = File(carpeta(context), "0601${mes.replace("-", "")}$ruc.jor")
        val texto = resumenes.joinToString("\r\n") { r ->
            val ordMin = (r.boleta.horasOrdinarias * 60).toLong()
            val extraMin = ((r.boleta.horasExtra25 + r.boleta.horasExtra35) * 60).toLong()
            // Tabla 3 de SUNAT: 01 = DNI, 04 = Carnet de extranjería
            val tipoDoc = if (r.usuario.tipoDocumento == com.juanreyna.registrotiempo.domain.model.TipoDocumento.DNI) "01" else "04"
            "$tipoDoc|${r.usuario.dni}|${ordMin / 60}|${ordMin % 60}|${extraMin / 60}|${extraMin % 60}|"
        }
        f.writeText(texto)
        return f
    }
}
