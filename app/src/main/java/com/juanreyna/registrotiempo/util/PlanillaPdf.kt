package com.juanreyna.registrotiempo.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.juanreyna.registrotiempo.domain.calc.FilaPlanilla
import com.juanreyna.registrotiempo.domain.calc.ParametrosLegales
import com.juanreyna.registrotiempo.domain.calc.PlanillaAvanzada
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.calc.ResumenMensual
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.SistemaPension
import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import java.io.File

/**
 * Documentos PDF del Modo Avanzado Perú:
 * - Parámetros legales (RMV, UIT, tasas, factores y tramos de 5.ª con su base legal).
 * - Planilla mensual (datos y asistencia; ingresos y descuentos; aportes, provisiones y costo; resumen contable y firmas).
 * - Boleta de pago individual (D.S. N° 001-98-TR).
 * La "planilla completa" reúne los tres en un solo PDF: planilla, parámetros y una boleta por colaborador.
 */
object PlanillaPdf {

    private val AZUL = Color.rgb(0x13, 0x29, 0x4B)
    private val VERDE = Color.rgb(0x0F, 0x8B, 0x6D)
    private val GRIS = Color.rgb(0xF1, 0xF5, 0xF9)
    private val GRIS_TOTAL = Color.rgb(0xE2, 0xE8, 0xF0)
    private val BORDE = Color.rgb(0xCB, 0xD5, 0xE1)
    private val TEXTO = Color.rgb(0x0F, 0x17, 0x2A)
    private val SECUNDARIO = Color.rgb(0x47, 0x55, 0x69)

    // =====================================================================================
    // Lienzo con tablas
    // =====================================================================================
    private class Doc(val empresa: Empresa, val periodo: String) {
        val pdf = PdfDocument()
        var ancho = 595f; var alto = 842f
        val margen = 28f
        var y = 0f
        private var numero = 0
        private var pagina: PdfDocument.Page? = null
        val util get() = ancho - 2 * margen
        val canvas get() = pagina!!.canvas

        fun pintura(tam: Float, negrita: Boolean = false, color: Int = TEXTO, cursiva: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tam; this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, when {
                negrita && cursiva -> Typeface.BOLD_ITALIC; negrita -> Typeface.BOLD; cursiva -> Typeface.ITALIC; else -> Typeface.NORMAL
            })
        }

        fun nuevaPagina(horizontal: Boolean) {
            cerrarPagina()
            numero++
            ancho = if (horizontal) 842f else 595f; alto = if (horizontal) 595f else 842f
            pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ancho.toInt(), alto.toInt(), numero).create())
            y = margen
        }

        private fun cerrarPagina() {
            val p = pagina ?: return
            val pie = pintura(6.5f, color = SECUNDARIO)
            p.canvas.drawText("${empresa.razonSocial.ifBlank { empresa.nombreComercial }} · $periodo · Marcaya v1.0.0", margen, alto - 14f, pie)
            val t = "Página $numero"
            p.canvas.drawText(t, ancho - margen - pie.measureText(t), alto - 14f, pie)
            pdf.finishPage(p)
            pagina = null
        }

        /** Salta de página si no caben [h] puntos más; devuelve true si saltó. */
        fun espacio(h: Float, horizontal: Boolean): Boolean {
            if (y + h <= alto - margen - 16f) return false
            nuevaPagina(horizontal); return true
        }

        fun recortar(t: String, p: Paint, w: Float): String {
            if (p.measureText(t) <= w) return t
            var s = t
            while (s.isNotEmpty() && p.measureText("$s…") > w) s = s.dropLast(1)
            return "$s…"
        }

        fun envolver(t: String, p: Paint, w: Float): List<String> {
            val lineas = mutableListOf<String>()
            t.split("\n").forEach { parrafo ->
                var actual = ""
                parrafo.split(" ").forEach { palabra ->
                    val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                    if (p.measureText(prueba) <= w || actual.isEmpty()) actual = prueba else { lineas += actual; actual = palabra }
                }
                lineas += actual
            }
            return lineas
        }

        fun celda(x: Float, yy: Float, w: Float, h: Float, t: String, p: Paint, fondo: Int? = null, alin: Paint.Align = Paint.Align.LEFT, borde: Boolean = true) {
            if (fondo != null) canvas.drawRect(x, yy, x + w, yy + h, Paint().apply { color = fondo; style = Paint.Style.FILL })
            if (borde) canvas.drawRect(x, yy, x + w, yy + h, Paint().apply { color = BORDE; style = Paint.Style.STROKE; strokeWidth = 0.6f })
            val s = recortar(t, p, w - 6f)
            val base = yy + h / 2 + p.textSize * 0.35f
            val tx = when (alin) { Paint.Align.RIGHT -> x + w - 3f - p.measureText(s); Paint.Align.CENTER -> x + (w - p.measureText(s)) / 2; else -> x + 3f }
            canvas.drawText(s, tx, base, p)
        }

        /** Celda con texto en varias líneas (encabezados). */
        fun celdaMultilinea(x: Float, yy: Float, w: Float, h: Float, lineas: List<String>, p: Paint, fondo: Int) {
            canvas.drawRect(x, yy, x + w, yy + h, Paint().apply { color = fondo; style = Paint.Style.FILL })
            canvas.drawRect(x, yy, x + w, yy + h, Paint().apply { color = BORDE; style = Paint.Style.STROKE; strokeWidth = 0.6f })
            val lh = p.textSize * 1.15f
            var by = yy + (h - lh * lineas.size) / 2 + p.textSize * 0.9f
            lineas.forEach { l -> canvas.drawText(recortar(l, p, w - 4f), x + (w - p.measureText(recortar(l, p, w - 4f))) / 2, by, p); by += lh }
        }

        fun barra(t: String, horizontal: Boolean, color: Int = AZUL, tam: Float = 9f) {
            espacio(18f, horizontal)
            celda(margen, y, util, 16f, t, pintura(tam, true, Color.WHITE), color, borde = false)
            y += 18f
        }

        fun parrafo(t: String, p: Paint, horizontal: Boolean, sangria: Float = 0f) {
            envolver(t, p, util - sangria).forEach { l ->
                espacio(p.textSize * 1.4f, horizontal)
                canvas.drawText(l, margen + sangria, y + p.textSize, p)
                y += p.textSize * 1.35f
            }
        }

        class Col(val titulo: String, val peso: Float, val alin: Paint.Align = Paint.Align.RIGHT)

        /** Tabla con encabezado (repetido en cada página), filas cebra y fila de totales opcional. */
        fun tabla(cols: List<Col>, filas: List<List<String>>, totales: List<String>? = null, tam: Float = 6.6f, horizontal: Boolean) {
            val total = cols.sumOf { it.peso.toDouble() }.toFloat()
            val anchos = cols.map { it.peso / total * util }
            val pEnc = pintura(tam, true, Color.WHITE)
            val lineasEnc = cols.mapIndexed { i, c -> envolver(c.titulo, pEnc, anchos[i] - 4f).take(3) }
            val hEnc = (lineasEnc.maxOf { it.size } * tam * 1.15f + 6f).coerceAtLeast(16f)
            val hFila = tam + 7f
            val pNormal = pintura(tam); val pNegrita = pintura(tam, true)
            fun encabezado() {
                var x = margen
                cols.indices.forEach { i -> celdaMultilinea(x, y, anchos[i], hEnc, lineasEnc[i], pEnc, VERDE); x += anchos[i] }
                y += hEnc
            }
            espacio(hEnc + hFila * 2, horizontal)
            encabezado()
            filas.forEachIndexed { n, f ->
                if (espacio(hFila, horizontal)) encabezado()
                var x = margen
                cols.indices.forEach { i ->
                    celda(x, y, anchos[i], hFila, f.getOrElse(i) { "" }, pNormal, if (n % 2 == 1) GRIS else null, cols[i].alin); x += anchos[i]
                }
                y += hFila
            }
            if (totales != null) {
                if (espacio(hFila + 2, horizontal)) encabezado()
                var x = margen
                cols.indices.forEach { i -> celda(x, y, anchos[i], hFila + 2, totales.getOrElse(i) { "" }, pNegrita, GRIS_TOTAL, cols[i].alin); x += anchos[i] }
                y += hFila + 2
            }
            y += 8f
        }

        fun guardar(archivo: File) {
            cerrarPagina()
            archivo.outputStream().use { pdf.writeTo(it) }
            pdf.close()
        }
    }

    private fun m(v: Double) = "%,.2f".format(v)
    private fun mz(v: Double) = if (v == 0.0) "—" else "%,.2f".format(v)
    private fun h(v: Double) = "%.2f".format(v)
    private fun fmtH(v: Double) = if (v == Math.floor(v)) "%.0f".format(v) else "%.1f".format(v)
    private fun fecha(iso: String?) = iso?.split("-")?.takeIf { it.size == 3 }?.reversed()?.joinToString("/") ?: "—"
    private fun razon(e: Empresa) = e.razonSocial.ifBlank { e.nombreComercial }
    private fun abrevDoc(t: TipoDocumento) = if (t == TipoDocumento.DNI) "DNI" else "C.E."
    private fun pension(s: SistemaPension) = s.etiqueta.removePrefix("AFP ").let { if (s == SistemaPension.ONP) "ONP" else "AFP $it" }

    private fun encabezadoDocumento(d: Doc, titulo: String, subtitulo: String, horizontal: Boolean) {
        val e = d.empresa
        d.celda(d.margen, d.y, d.util, 24f, titulo, d.pintura(12f, true, Color.WHITE), AZUL, Paint.Align.CENTER, borde = false)
        d.y += 30f
        val p = d.pintura(8f, true); val s = d.pintura(7.5f, color = SECUNDARIO)
        d.canvas.drawText("Empleador: ${razon(e)}   ·   RUC: ${e.ruc.ifBlank { "— (registrar en Reportes → Datos del empleador)" }}", d.margen, d.y + 8f, p)
        val per = "Periodo: ${d.periodo}"
        d.canvas.drawText(per, d.margen + d.util - p.measureText(per), d.y + 8f, p)
        d.y += 13f
        d.canvas.drawText(subtitulo, d.margen, d.y + 8f, s)
        d.y += 16f
    }

    // =====================================================================================
    // Documentos
    // =====================================================================================
    /** Planilla completa: planilla mensual + parámetros legales + boletas individuales. */
    fun completa(archivo: File, empresa: Empresa, resumenes: List<ResumenColaborador>, mes: String, generadoEl: String) {
        val filas = PlanillaAvanzada.filas(resumenes)
        require(filas.isNotEmpty()) { "no hay colaboradores en Modo Avanzado en este periodo" }
        val d = Doc(empresa, Fechas.etiquetaMes(mes))
        planilla(d, filas, generadoEl)
        parametros(d, ParametrosLegales.delMes(mes), mes)
        filas.forEach { boleta(d, it, mes) }
        d.guardar(archivo)
    }

    fun soloParametros(archivo: File, empresa: Empresa, mes: String) {
        val d = Doc(empresa, Fechas.etiquetaMes(mes))
        parametros(d, ParametrosLegales.delMes(mes), mes)
        d.guardar(archivo)
    }

    fun soloPlanilla(archivo: File, empresa: Empresa, resumenes: List<ResumenColaborador>, mes: String, generadoEl: String) {
        val filas = PlanillaAvanzada.filas(resumenes)
        require(filas.isNotEmpty()) { "no hay colaboradores en Modo Avanzado en este periodo" }
        val d = Doc(empresa, Fechas.etiquetaMes(mes))
        planilla(d, filas, generadoEl)
        d.guardar(archivo)
    }

    fun boletaIndividual(archivo: File, empresa: Empresa, resumen: ResumenColaborador, mes: String) {
        val d = Doc(empresa, Fechas.etiquetaMes(mes))
        boleta(d, PlanillaAvanzada.fila(1, resumen), mes)
        d.guardar(archivo)
    }

    // ---------- Planilla mensual (horizontal) ----------
    private fun planilla(d: Doc, filas: List<FilaPlanilla>, generadoEl: String) {
        val H = true
        d.nuevaPagina(H)
        encabezadoDocumento(
            d, "PLANILLA DE REMUNERACIONES MENSUAL — MODO AVANZADO PERÚ",
            "Moneda: ${if (d.empresa.moneda.simbolo == "S/") "Soles (S/)" else "Dólares (US$)"} · ${filas.size} ${if (filas.size == 1) "trabajador" else "trabajadores"} · " +
                "Generado el ${fecha(generadoEl)} con los marcajes registrados a la fecha (proyección). Base para la declaración en el PLAME.",
            H
        )
        val L = Paint.Align.LEFT; val CT = Paint.Align.CENTER

        d.barra("1. DATOS DEL TRABAJADOR, VÍNCULO Y ASISTENCIA", H)
        d.tabla(
            listOf(
                Doc.Col("N°", 2.2f, CT), Doc.Col("Tipo doc.", 3f, CT), Doc.Col("N° documento", 6f, CT), Doc.Col("Apellidos y nombres", 17f, L),
                Doc.Col("Cargo / puesto", 11f, L), Doc.Col("C. costo", 5f, CT), Doc.Col("Fecha ingreso", 5.5f, CT), Doc.Col("Régimen laboral", 7f, CT),
                Doc.Col("Tipo remun.", 5f, CT), Doc.Col("Sistema pensión", 7f, CT), Doc.Col("CUSPP", 7f, CT),
                Doc.Col("Días comp.", 3.6f, CT), Doc.Col("Días pag.", 3.6f, CT), Doc.Col("Faltas", 3.4f, CT), Doc.Col("Min. tard.", 3.6f, CT),
                Doc.Col("Horas ord.", 4.4f), Doc.Col("H.E. 25%", 4f), Doc.Col("H.E. 35%", 4f)
            ),
            filas.map { f ->
                val c = f.usuario.configPago; val b = f.boleta
                listOf(
                    "${f.numero}", abrevDoc(f.usuario.tipoDocumento), f.documento, f.nombre, c.cargo.ifBlank { "—" }, c.centroCosto.ifBlank { "—" },
                    fecha(ResumenMensual.fechaIngreso(f.usuario)), PlanillaAvanzada.etiquetaRegimen(c.regimen), if (f.porHora) "Por hora" else "Mensual",
                    pension(c.sistemaPension) + if (c.comisionMixta && c.sistemaPension != SistemaPension.ONP) " (mixta)" else "",
                    if (c.sistemaPension == SistemaPension.ONP) "—" else c.cuspp.ifBlank { "—" },
                    "${f.diasComputables}", "${b.diasLaborados ?: 0}", "${b.faltas}", "${b.minutosTardanza}",
                    h(b.horasOrdinarias), h(b.horasExtra25), h(b.horasExtra35)
                )
            },
            listOf("", "", "", "TOTALES", "", "", "", "", "", "", "", "", "${filas.sumOf { it.boleta.diasLaborados ?: 0 }}",
                "${filas.sumOf { it.boleta.faltas }}", "${filas.sumOf { it.boleta.minutosTardanza }}",
                h(filas.sumOf { it.boleta.horasOrdinarias }), h(filas.sumOf { it.boleta.horasExtra25 }), h(filas.sumOf { it.boleta.horasExtra35 })),
            horizontal = H
        )

        d.barra("2. INGRESOS DEVENGADOS, DESCUENTOS AL TRABAJADOR Y NETO (${d.empresa.moneda.simbolo})", H)
        d.tabla(
            listOf(
                Doc.Col("N°", 2.2f, CT), Doc.Col("Apellidos y nombres", 15f, L), Doc.Col("Remuneración básica", 6f), Doc.Col("Asig. familiar", 5f),
                Doc.Col("H.E. 25%", 5f), Doc.Col("H.E. 35%", 5f), Doc.Col("(−) Tardanzas", 5f), Doc.Col("TOTAL INGRESOS", 6.4f),
                Doc.Col("ONP 13%", 5f), Doc.Col("AFP fondo 10%", 5.4f), Doc.Col("AFP prima seguro", 5.4f), Doc.Col("AFP comisión", 5f),
                Doc.Col("Renta 5.ª cat.", 5f), Doc.Col("TOTAL DESCUENTOS", 6.4f), Doc.Col("NETO A PAGAR", 6.6f)
            ),
            filas.map { f ->
                listOf("${f.numero}", f.nombre, m(f.basica), mz(f.asignacionFamiliar), mz(f.extras25), mz(f.extras35), mz(f.tardanzas), m(f.ingresos),
                    mz(f.onp), mz(f.afpFondo), mz(f.afpPrima), mz(f.afpComision), mz(f.quinta), m(f.descuentos), m(f.neto))
            },
            listOf("", "TOTALES") + listOf<(FilaPlanilla) -> Double>(
                { it.basica }, { it.asignacionFamiliar }, { it.extras25 }, { it.extras35 }, { it.tardanzas }, { it.ingresos },
                { it.onp }, { it.afpFondo }, { it.afpPrima }, { it.afpComision }, { it.quinta }, { it.descuentos }, { it.neto }
            ).map { sel -> m(filas.sumOf(sel)) },
            horizontal = H
        )

        d.barra("3. APORTES DEL EMPLEADOR, PROVISIONES DE BENEFICIOS SOCIALES Y COSTO LABORAL (${d.empresa.moneda.simbolo})", H)
        d.tabla(
            listOf(
                Doc.Col("N°", 2.2f, CT), Doc.Col("Apellidos y nombres", 16f, L), Doc.Col("Régimen", 7f, CT), Doc.Col("TOTAL INGRESOS", 6.5f),
                Doc.Col("EsSalud 9%", 6f), Doc.Col("Gratificación", 6f), Doc.Col("Bonif. extra. 9%", 6f), Doc.Col("CTS", 6f),
                Doc.Col("Vacaciones", 6f), Doc.Col("TOTAL PROVISIONES", 6.5f), Doc.Col("COSTO TOTAL EMPRESA", 7f)
            ),
            filas.map { f ->
                listOf("${f.numero}", f.nombre, PlanillaAvanzada.etiquetaRegimen(f.usuario.configPago.regimen), m(f.ingresos),
                    if (f.usuario.configPago.regimen == com.juanreyna.registrotiempo.domain.model.RegimenLaboral.MICROEMPRESA) "SIS" else m(f.essalud),
                    mz(f.gratificacion), mz(f.bonificacion), mz(f.cts), mz(f.vacaciones), m(f.provisiones), m(f.costo))
            },
            listOf("", "TOTALES", "") + listOf<(FilaPlanilla) -> Double>(
                { it.ingresos }, { it.essalud }, { it.gratificacion }, { it.bonificacion }, { it.cts }, { it.vacaciones }, { it.provisiones }, { it.costo }
            ).map { sel -> m(filas.sumOf(sel)) },
            horizontal = H
        )

        // Resumen contable y tesorería
        val s = d.empresa.moneda.simbolo
        val resumen = listOf(
            "Total remuneraciones brutas devengadas" to filas.sumOf { it.ingresos },
            "Total descuentos ONP (a declarar en el PLAME)" to filas.sumOf { it.onp },
            "Total descuentos AFP (a pagar en AFPnet)" to filas.sumOf { it.afpFondo + it.afpPrima + it.afpComision },
            "Total retención de renta de 5.ª categoría" to filas.sumOf { it.quinta },
            "TOTAL NETO A ABONAR EN CUENTAS SUELDO" to filas.sumOf { it.neto },
            "Total aportes del empleador (EsSalud)" to filas.sumOf { it.aportes },
            "Total provisiones (gratificación, bonificación, CTS y vacaciones)" to filas.sumOf { it.provisiones },
            "COSTO LABORAL TOTAL DEL PERIODO" to filas.sumOf { it.costo }
        )
        d.espacio(16f * resumen.size + 40f, H)
        d.barra("4. RESUMEN CONTABLE Y TESORERÍA", H)
        val wl = d.util * 0.42f; val wv = d.util * 0.14f
        resumen.forEach { (t, v) ->
            val fuerte = t.startsWith("TOTAL NETO") || t.startsWith("COSTO")
            val p = d.pintura(7.5f, fuerte)
            d.celda(d.margen, d.y, wl, 15f, t, p, if (fuerte) GRIS_TOTAL else null)
            d.celda(d.margen + wl, d.y, wv, 15f, "$s ${m(v)}", p, if (fuerte) GRIS_TOTAL else null, Paint.Align.RIGHT)
            d.y += 15f
        }
        d.y += 8f

        val obs = filas.filter { it.boleta.advertencias.isNotEmpty() }
        if (obs.isNotEmpty()) {
            d.barra("5. OBSERVACIONES", H, SECUNDARIO, 8f)
            val p = d.pintura(6.8f)
            obs.forEach { f -> d.parrafo("${f.numero}. ${f.nombre}: ${f.boleta.advertencias.joinToString(" ")}", p, H) }
            d.y += 6f
        }
        d.parrafo(
            "Notas: la 5.ª categoría es una proyección (renta anual ÷ 12); en la planilla oficial se aplica el art. 40 del Reglamento de la LIR. " +
                "Las provisiones no se pagan en el mes: la gratificación en julio y diciembre, la CTS en mayo y noviembre y las vacaciones al gozarlas. " +
                "No incluye SCTR ni Seguro Vida Ley (dependen de la póliza de la empresa), bonos ni adelantos.",
            d.pintura(6.5f, color = SECUNDARIO, cursiva = true), H
        )
        firmas(d, listOf("Elaborado por: Recursos Humanos", "Revisado por: Contabilidad", "Aprobado por: Gerencia General"), H)
    }

    private fun firmas(d: Doc, textos: List<String>, horizontal: Boolean) {
        d.espacio(60f, horizontal)
        d.y += 38f
        val w = d.util / textos.size
        val linea = Paint().apply { color = TEXTO; strokeWidth = 0.7f }
        val p = d.pintura(7.5f)
        textos.forEachIndexed { i, t ->
            val x0 = d.margen + i * w + 18f; val x1 = d.margen + (i + 1) * w - 18f
            d.canvas.drawLine(x0, d.y, x1, d.y, linea)
            d.envolver(t, p, x1 - x0).forEachIndexed { k, l -> d.canvas.drawText(l, x0 + (x1 - x0 - p.measureText(l)) / 2, d.y + 10f + k * 9f, p) }
        }
        d.y += 26f
    }

    // ---------- Parámetros legales (vertical) ----------
    private fun parametros(d: Doc, p: ParametrosLegales, mes: String) {
        val H = false
        d.nuevaPagina(H)
        encabezadoDocumento(d, "PARÁMETROS LEGALES, LABORALES Y TRIBUTARIOS — PERÚ",
            "Valores con los que la app calcula la planilla y las boletas de ${Fechas.etiquetaMes(mes)}. Revíselos cada año.", H)
        val rmvBase = if (p.rmv >= 1230.0) "D.S. N° 015-2026-TR (desde el 01/10/2026)" else "D.S. N° 006-2024-TR (desde el 01/01/2025)"
        val uitBase = if (p.uit >= 5500.0) "D.S. N° 301-2025-EF (UIT 2026)" else "D.S. N° 260-2024-EF (UIT 2025)"
        fun pct(v: Double) = "%.2f %%".format(v * 100).replace(".00 %", " %")
        val s = d.empresa.moneda.simbolo
        val L = Paint.Align.LEFT; val CT = Paint.Align.CENTER
        val cols = listOf(Doc.Col("Parámetro / concepto", 34f, L), Doc.Col("Valor", 11f, CT), Doc.Col("Base legal / observación", 55f, L))
        val r = p.comisionFlujo
        d.barra("REMUNERACIÓN Y JORNADA", H)
        d.tabla(cols, listOf(
            listOf("Remuneración Mínima Vital (RMV)", "$s ${m(p.rmv)}", rmvBase),
            listOf("Asignación familiar", "$s ${m(p.rmv * 0.10)}", "Ley N° 25129: 10 % de la RMV (hijos menores o en estudios superiores)"),
            listOf("Unidad Impositiva Tributaria (UIT)", "$s ${m(p.uit)}", uitBase),
            listOf("Días del mes para la remuneración", "${p.diasMes}", "Mes comercial de 30 días (incluye descansos semanales)"),
            listOf("Jornada diaria ordinaria", "${fmtH(p.jornadaDiariaBase.toDouble())} h", "Máximo 8 h diarias o 48 h semanales (Constitución, art. 25)"),
            listOf("Horas mensuales base (valor hora)", "${fmtH(p.horasMensualesBase.toDouble())} h", "Valor hora = remuneración mensual ÷ 240"),
            listOf("Horas extras: 2 primeras horas del día", "+25 %", "D.S. N° 007-2002-TR, art. 10"),
            listOf("Horas extras: desde la 3.ª hora", "+35 %", "D.S. N° 007-2002-TR, art. 10"),
            listOf("Trabajo nocturno (10 p. m. – 6 a. m.)", "RMV +35 %", "D.S. N° 007-2002-TR, art. 8 (referencial; no se calcula automáticamente)"),
            listOf("Tiempo parcial", "< 4 h/día", "Sin CTS ni vacaciones (D.S. N° 001-97-TR, art. 4; D. Leg. N° 713, art. 12)")
        ), tam = 7.2f, horizontal = H)
        d.barra("SISTEMAS DE PENSIONES (DESCUENTO AL TRABAJADOR)", H)
        d.tabla(cols, listOf(
            listOf("ONP — Sistema Nacional de Pensiones", pct(p.tasaOnp), "D.L. N° 19990"),
            listOf("AFP — Aporte obligatorio al fondo", pct(p.aporteAfp), "T.U.O. de la Ley del SPP (D.S. N° 054-97-EF)"),
            listOf("AFP — Prima de seguro", pct(p.primaSeguroAfp), "Tasa SBS; se aplica hasta la remuneración máxima asegurable"),
            listOf("AFP — Remuneración máxima asegurable", "$s ${m(p.remuneracionMaximaAsegurable)}", "Tope de la prima de seguro (SBS)"),
            listOf("AFP Integra — Comisión sobre flujo", pct(r[SistemaPension.AFP_INTEGRA] ?: 0.0), "0 % si el afiliado está en comisión mixta"),
            listOf("AFP Prima — Comisión sobre flujo", pct(r[SistemaPension.AFP_PRIMA] ?: 0.0), "0 % si el afiliado está en comisión mixta"),
            listOf("AFP Profuturo — Comisión sobre flujo", pct(r[SistemaPension.AFP_PROFUTURO] ?: 0.0), "0 % si el afiliado está en comisión mixta"),
            listOf("AFP Hábitat — Comisión sobre flujo", pct(r[SistemaPension.AFP_HABITAT] ?: 0.0), "0 % si el afiliado está en comisión mixta")
        ), tam = 7.2f, horizontal = H)
        d.barra("APORTES DEL EMPLEADOR Y BENEFICIOS SOCIALES", H)
        d.tabla(cols, listOf(
            listOf("EsSalud", pct(p.tasaEssalud), "Ley N° 26790; base mínima: una RMV. La microempresa REMYPE afilia al SIS"),
            listOf("Gratificaciones (julio y diciembre)", "1 sueldo c/u", "Ley N° 27735: provisión de 1/6 por mes. Pequeña empresa: media; microempresa: no"),
            listOf("Bonificación extraordinaria", pct(p.bonificacionExtraordinaria), "Ley N° 30334: sobre la gratificación"),
            listOf("CTS (mayo y noviembre)", "1/12 anual", "D.S. N° 001-97-TR: (rem. + 1/6 grati.) ÷ 12. Pequeña empresa: 15 días/año"),
            listOf("Vacaciones", "30 días", "D. Leg. N° 713. REMYPE (micro y pequeña): 15 días al año"),
            listOf("SCTR salud y pensión", "Según póliza", "Solo actividades de riesgo (Anexo 5, D.S. N° 009-97-SA). No calculado por la app"),
            listOf("Seguro Vida Ley", "Según póliza", "D. Leg. N° 688 (D.U. N° 044-2019): desde el primer día. No calculado por la app")
        ), tam = 7.2f, horizontal = H)
        d.barra("RENTA DE 5.ª CATEGORÍA (RETENCIÓN AL TRABAJADOR)", H)
        val limites = listOf(0.0) + p.tramosQuinta.dropLast(1).map { it.first }
        fun uit(v: Double) = if (v == Math.floor(v)) "%.0f".format(v) else "$v"
        d.tabla(
            listOf(Doc.Col("Tramo de renta neta anual", 45f, L), Doc.Col("Tasa", 12f, CT), Doc.Col("Desde (S/)", 20f), Doc.Col("Hasta (S/)", 23f)),
            listOf(listOf("Deducción anual: ${uit(p.deduccionQuintaUit)} UIT", "—", "—", "$s ${m(p.deduccionQuintaUit * p.uit)}")) +
                p.tramosQuinta.mapIndexed { i, (hasta, tasa) ->
                    val desde = limites[i]
                    listOf(
                        if (hasta == Double.MAX_VALUE) "Más de ${uit(desde)} UIT" else "Más de ${uit(desde)} hasta ${uit(hasta)} UIT",
                        pct(tasa), m(desde * p.uit), if (hasta == Double.MAX_VALUE) "En adelante" else m(hasta * p.uit)
                    )
                },
            tam = 7.2f, horizontal = H
        )
        d.parrafo(
            "Art. 53 de la Ley del Impuesto a la Renta. La app proyecta la renta anual (remuneración × 12 + gratificaciones + bonificación + extras del mes), " +
                "resta 7 UIT, aplica los tramos y retiene 1/12 por mes. En la planilla oficial se usa el procedimiento del art. 40 del Reglamento.",
            d.pintura(6.8f, color = SECUNDARIO, cursiva = true), H
        )
    }

    // ---------- Boleta individual (vertical) ----------
    private fun boleta(d: Doc, f: FilaPlanilla, mes: String) {
        val H = false
        val u = f.usuario; val c = u.configPago; val b = f.boleta
        val s = d.empresa.moneda.simbolo
        d.nuevaPagina(H)
        d.celda(d.margen, d.y, d.util, 24f, "BOLETA DE PAGO DE REMUNERACIONES", d.pintura(13f, true, Color.WHITE), AZUL, Paint.Align.CENTER, borde = false)
        d.y += 26f
        d.celda(d.margen, d.y, d.util, 13f, "D.S. N° 001-98-TR · Periodo: ${Fechas.etiquetaMes(mes)}", d.pintura(7.5f, color = SECUNDARIO), alin = Paint.Align.CENTER, borde = false)
        d.y += 18f

        // Datos del empleador y del trabajador
        val pares = listOf(
            "Empleador" to razon(d.empresa), "RUC" to d.empresa.ruc.ifBlank { "—" },
            "Trabajador" to f.nombre, "Documento" to "${u.tipoDocumento.etiqueta} ${f.documento}",
            "Cargo / puesto" to c.cargo.ifBlank { "—" }, "Fecha de ingreso" to fecha(ResumenMensual.fechaIngreso(u)),
            "Régimen laboral" to PlanillaAvanzada.etiquetaRegimen(c.regimen), "Tipo de remuneración" to (if (f.porHora) "Por hora" else "Mensual"),
            "Régimen pensionario" to pension(c.sistemaPension) + if (c.comisionMixta && c.sistemaPension != SistemaPension.ONP) " (comisión mixta)" else "",
            "CUSPP" to (if (c.sistemaPension == SistemaPension.ONP) "—" else c.cuspp.ifBlank { "—" }),
            "Remuneración pactada" to (if (f.porHora) "$s ${m(c.tarifaPorHora)} por hora" else "$s ${m(c.sueldoBase)} mensual"),
            "Centro de costo" to c.centroCosto.ifBlank { "—" },
            "Días laborados / faltas" to "${b.diasLaborados ?: 0} días · ${b.faltas} ${if (b.faltas == 1) "falta" else "faltas"}",
            "Horas ordinarias / extras" to "${h(b.horasOrdinarias)} h · ${h(b.horasExtra25 + b.horasExtra35)} h extras"
        )
        val wEt = d.util * 0.17f; val wVal = d.util * 0.33f
        val pEt = d.pintura(7.3f, true); val pVal = d.pintura(7.3f)
        pares.chunked(2).forEach { fila ->
            fila.forEachIndexed { i, (et, v) ->
                val x = d.margen + i * (wEt + wVal)
                d.celda(x, d.y, wEt, 15f, et, pEt, GRIS)
                d.celda(x + wEt, d.y, wVal, 15f, v, pVal)
            }
            d.y += 15f
        }
        d.y += 10f

        // Ingresos | Descuentos | Aportes
        val ing = b.ingresos.map { it.concepto to it.monto }
        // "AFP Integra — Comisión sobre flujo (1.55 %)" → "AFP — Comisión sobre flujo (1.55 %)": la AFP ya figura arriba
        val des = b.descuentos.map { it.concepto.replace(Regex("^AFP [^—]+—"), "AFP —") to it.monto }
        val apo = b.aportesEmpleador.map { it.concepto to it.monto }
        val n = maxOf(ing.size, des.size, apo.size)
        val wc = d.util / 3f; val wm = 52f
        val pEnc = d.pintura(7.5f, true, Color.WHITE)
        listOf("INGRESOS", "DESCUENTOS DEL TRABAJADOR", "APORTES DEL EMPLEADOR").forEachIndexed { i, t ->
            d.celda(d.margen + i * wc, d.y, wc - wm, 16f, t, pEnc, VERDE)
            d.celda(d.margen + i * wc + wc - wm, d.y, wm, 16f, "IMPORTE", pEnc, VERDE, Paint.Align.RIGHT)
        }
        d.y += 16f
        val pL = d.pintura(6.6f)
        for (k in 0 until n) {
            listOf(ing, des, apo).forEachIndexed { i, lista ->
                val (t, v) = lista.getOrNull(k) ?: ("" to null)
                d.celda(d.margen + i * wc, d.y, wc - wm, 14f, t, pL)
                d.celda(d.margen + i * wc + wc - wm, d.y, wm, 14f, v?.let { m(it) } ?: "", pL, alin = Paint.Align.RIGHT)
            }
            d.y += 14f
        }
        val pT = d.pintura(7.3f, true)
        listOf("TOTAL INGRESOS" to b.totalIngresos, "TOTAL DESCUENTOS" to b.totalDescuentos, "TOTAL APORTES" to f.aportes).forEachIndexed { i, (t, v) ->
            d.celda(d.margen + i * wc, d.y, wc - wm, 16f, t, pT, GRIS_TOTAL)
            d.celda(d.margen + i * wc + wc - wm, d.y, wm, 16f, m(v), pT, GRIS_TOTAL, Paint.Align.RIGHT)
        }
        d.y += 24f

        // Neto
        d.celda(d.margen, d.y, d.util * 0.7f, 24f, "NETO A RECIBIR POR EL TRABAJADOR", d.pintura(10f, true, Color.WHITE), VERDE, borde = false)
        d.celda(d.margen + d.util * 0.7f, d.y, d.util * 0.3f, 24f, "$s ${m(b.netoAPagar)}", d.pintura(12f, true, Color.WHITE), VERDE, Paint.Align.RIGHT, borde = false)
        d.y += 32f

        // Provisiones
        if (b.provisiones.isNotEmpty()) {
            d.barra("PROVISIONES DEL EMPLEADOR (informativo: no se pagan en este mes)", H, SECUNDARIO, 7.5f)
            val wp = d.util * 0.5f
            b.provisiones.forEach { l ->
                d.celda(d.margen, d.y, wp, 14f, l.concepto, pL)
                d.celda(d.margen + wp, d.y, wp * 0.3f, 14f, m(l.monto), pL, alin = Paint.Align.RIGHT)
                d.y += 14f
            }
            d.celda(d.margen, d.y, wp, 15f, "Costo laboral total del empleador", pT, GRIS_TOTAL)
            d.celda(d.margen + wp, d.y, wp * 0.3f, 15f, m(b.costoEmpleador), pT, GRIS_TOTAL, Paint.Align.RIGHT)
            d.y += 22f
        }

        if (b.advertencias.isNotEmpty()) {
            d.canvas.drawText("Observaciones", d.margen, d.y + 8f, d.pintura(7.5f, true)); d.y += 12f
            b.advertencias.forEach { d.parrafo("• $it", d.pintura(6.8f, color = SECUNDARIO), H, 4f) }
            d.y += 4f
        }
        d.canvas.drawText("Fecha de entrega: ____ / ____ / ________", d.margen, d.y + 10f, d.pintura(7.5f)); d.y += 14f
        firmas(d, listOf("Firma y sello del empleador o representante", "Recibí conforme — firma y huella digital del trabajador"), H)
        d.parrafo(
            "Boleta emitida conforme al D.S. N° 001-98-TR; puede entregarse en físico o por medios electrónicos con firma digital (D.S. N° 009-2011-TR). " +
                "Documento referencial calculado con los marcajes registrados en la app Marcaya.",
            d.pintura(6.3f, color = SECUNDARIO, cursiva = true), H
        )
    }
}
