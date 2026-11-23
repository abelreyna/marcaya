package com.juanreyna.registrotiempo.util

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escritor mínimo de libros Excel (.xlsx, Office Open XML) sin dependencias externas.
 * Soporta texto, números, fórmulas con valor en caché, celdas combinadas, anchos de columna, alto de filas,
 * paneles inmovilizados, listas desplegables (validación de datos) y un juego fijo de estilos.
 * Excel recalcula todas las fórmulas al abrir el archivo (fullCalcOnLoad).
 */
object Xlsx {

    /** Estilos disponibles (el índice es la posición en cellXfs de styles.xml). */
    enum class Estilo(val id: Int) {
        NORMAL(0), TITULO(1), NOTA(2), ENCABEZADO(3), TEXTO(4), DINERO(5),
        EDITABLE_TEXTO(6), EDITABLE_DINERO(7), EDITABLE_NUMERO(8), NUMERO(9),
        TOTAL_TEXTO(10), TOTAL_DINERO(11), EDITABLE_PORCENTAJE(12), ENTERO(13),
        NETO(14), SECCION(15), FIRMA(16), NEGRITA(17), EDITABLE_ENTERO(18),
        AUXILIAR(19), TOTAL_NUMERO(20), PORCENTAJE(21), NETO_TEXTO(22)
    }

    sealed class Celda(val estilo: Estilo) {
        class Texto(val v: String, e: Estilo) : Celda(e)
        class Numero(val v: Double, e: Estilo) : Celda(e)
        class Formula(val f: String, val cache: Double?, e: Estilo) : Celda(e)
        class FormulaTexto(val f: String, val cache: String, e: Estilo) : Celda(e)
        class Vacia(e: Estilo) : Celda(e)
    }

    class Hoja(val nombre: String) {
        internal val filas = sortedMapOf<Int, java.util.SortedMap<Int, Celda>>()
        internal val combinadas = mutableListOf<String>()
        internal val anchos = sortedMapOf<Int, Double>()
        internal val altos = mutableMapOf<Int, Double>()
        internal val listas = mutableListOf<Pair<String, String>>()
        /** Columnas y filas inmovilizadas (p. ej. 4 columnas y 5 filas). */
        var inmovilizar: Pair<Int, Int>? = null
        var horizontal = false
        var cuadricula = true

        private fun poner(ref: String, c: Celda) {
            val (col, fila) = ref(ref)
            filas.getOrPut(fila) { sortedMapOf() }[col] = c
        }

        fun texto(ref: String, v: String, e: Estilo = Estilo.TEXTO) = poner(ref, Celda.Texto(v, e))
        fun numero(ref: String, v: Double, e: Estilo = Estilo.NUMERO) = poner(ref, Celda.Numero(v, e))
        fun formula(ref: String, f: String, cache: Double?, e: Estilo = Estilo.DINERO) = poner(ref, Celda.Formula(f.removePrefix("="), cache, e))
        fun formulaTexto(ref: String, f: String, cache: String, e: Estilo = Estilo.TEXTO) = poner(ref, Celda.FormulaTexto(f.removePrefix("="), cache, e))
        fun vacia(ref: String, e: Estilo) = poner(ref, Celda.Vacia(e))

        /** Combina un rango y rellena con el estilo las celdas vacías (para que el borde y el color cubran todo el rango). */
        fun combinar(rango: String, e: Estilo? = null) {
            combinadas += rango
            if (e == null) return
            val (a, b) = rango.split(":")
            val (c1, f1) = ref(a); val (c2, f2) = ref(b)
            for (f in f1..f2) for (c in c1..c2) {
                val fila = filas.getOrPut(f) { sortedMapOf() }
                if (c !in fila) fila[c] = Celda.Vacia(e)
            }
        }

        fun ancho(col: String, ancho: Double) { anchos[colNum(col)] = ancho }
        fun alto(fila: Int, alto: Double) { altos[fila] = alto }
        /** Lista desplegable en [rango] con los valores de [origen] (p. ej. "Planilla!\$C\$6:\$C\$9" o "\"Sí,No\""). */
        fun lista(rango: String, origen: String) { listas += rango to origen }
    }

    // ---------- Referencias A1 ----------
    fun colLetra(n: Int): String {
        var x = n; val sb = StringBuilder()
        while (x > 0) { val r = (x - 1) % 26; sb.insert(0, ('A' + r)); x = (x - 1) / 26 }
        return sb.toString()
    }

    fun colNum(letras: String): Int = letras.fold(0) { acc, ch -> acc * 26 + (ch - 'A' + 1) }

    fun ref(ref: String): Pair<Int, Int> {
        val letras = ref.takeWhile { it.isLetter() }
        return colNum(letras) to ref.drop(letras.length).toInt()
    }

    // ---------- Escritura ----------
    fun escribir(out: OutputStream, hojas: List<Hoja>, monedaSimbolo: String = "S/") {
        ZipOutputStream(out).use { z ->
            fun put(nombre: String, xml: String) {
                z.putNextEntry(ZipEntry(nombre)); z.write(xml.toByteArray(Charsets.UTF_8)); z.closeEntry()
            }
            put("[Content_Types].xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
                hojas.indices.forEach { append("""<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""") }
                append("</Types>")
            })
            put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            put("xl/workbook.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><bookViews><workbookView activeTab="0"/></bookViews><sheets>""")
                hojas.forEachIndexed { i, h -> append("""<sheet name="${esc(h.nombre)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""") }
                append("""</sheets><calcPr calcId="191029" fullCalcOnLoad="1"/></workbook>""")
            })
            put("xl/_rels/workbook.xml.rels", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                hojas.indices.forEach { append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""") }
                append("""<Relationship Id="rId${hojas.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""")
            })
            put("xl/styles.xml", estilos(monedaSimbolo))
            hojas.forEachIndexed { i, h -> put("xl/worksheets/sheet${i + 1}.xml", hoja(h, i == 0)) }
        }
    }

    private fun hoja(h: Hoja, activa: Boolean): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>""")
        append("""<sheetViews><sheetView workbookViewId="0"${if (activa) " tabSelected=\"1\"" else ""}${if (!h.cuadricula) " showGridLines=\"0\"" else ""}>""")
        h.inmovilizar?.let { (c, f) ->
            val tl = colLetra(c + 1) + (f + 1)
            val pane = when { c > 0 && f > 0 -> "bottomRight"; c > 0 -> "topRight"; else -> "bottomLeft" }
            append("<pane")
            if (c > 0) append(""" xSplit="$c"""")
            if (f > 0) append(""" ySplit="$f"""")
            append(""" topLeftCell="$tl" activePane="$pane" state="frozen"/><selection pane="$pane" activeCell="$tl" sqref="$tl"/>""")
        }
        append("</sheetView></sheetViews>")
        append("""<sheetFormatPr defaultRowHeight="15"/>""")
        if (h.anchos.isNotEmpty()) {
            append("<cols>")
            h.anchos.forEach { (c, w) -> append("""<col min="$c" max="$c" width="$w" customWidth="1"/>""") }
            append("</cols>")
        }
        append("<sheetData>")
        val todas = (h.filas.keys + h.altos.keys).toSortedSet()
        for (f in todas) {
            append("""<row r="$f"""")
            h.altos[f]?.let { append(""" ht="$it" customHeight="1"""") }
            append(">")
            h.filas[f]?.forEach { (c, celda) -> append(celdaXml(colLetra(c) + f, celda)) }
            append("</row>")
        }
        append("</sheetData>")
        if (h.combinadas.isNotEmpty()) {
            append("""<mergeCells count="${h.combinadas.size}">""")
            h.combinadas.forEach { append("""<mergeCell ref="$it"/>""") }
            append("</mergeCells>")
        }
        if (h.listas.isNotEmpty()) {
            append("""<dataValidations count="${h.listas.size}">""")
            h.listas.forEach { (rango, origen) ->
                append("""<dataValidation type="list" allowBlank="1" showInputMessage="1" showErrorMessage="1" sqref="$rango"><formula1>${esc(origen)}</formula1></dataValidation>""")
            }
            append("</dataValidations>")
        }
        append("""<pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.3" footer="0.3"/>""")
        append("""<pageSetup paperSize="9" orientation="${if (h.horizontal) "landscape" else "portrait"}" fitToWidth="1" fitToHeight="0"/>""")
        append("</worksheet>")
    }

    private fun celdaXml(r: String, c: Celda): String {
        val s = c.estilo.id
        return when (c) {
            is Celda.Texto -> """<c r="$r" s="$s" t="inlineStr"><is><t xml:space="preserve">${esc(c.v)}</t></is></c>"""
            is Celda.Numero -> """<c r="$r" s="$s"><v>${num(c.v)}</v></c>"""
            is Celda.Formula -> """<c r="$r" s="$s"><f>${esc(c.f)}</f>${c.cache?.let { "<v>${num(it)}</v>" } ?: ""}</c>"""
            is Celda.FormulaTexto -> """<c r="$r" s="$s" t="str"><f>${esc(c.f)}</f><v>${esc(c.cache)}</v></c>"""
            is Celda.Vacia -> """<c r="$r" s="$s"/>"""
        }
    }

    private fun num(v: Double): String =
        if (v.isNaN() || v.isInfinite()) "0" else java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()

    internal fun esc(s: String): String = buildString {
        s.forEach { ch ->
            when {
                ch == '&' -> append("&amp;"); ch == '<' -> append("&lt;"); ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch < ' ' && ch != '\n' && ch != '\t' && ch != '\r' -> {}
                else -> append(ch)
            }
        }
    }

    private fun estilos(moneda: String): String {
        val fmtDinero = esc("\"${moneda} \"#,##0.00;[Red]\"-${moneda} \"#,##0.00")
        // Fuentes: 0 normal, 1 título blanco, 2 nota cursiva gris, 3 encabezado blanco, 4 negrita, 5 negrita blanca grande
        val fonts = listOf(
            """<font><sz val="10"/><color rgb="FF0F172A"/><name val="Calibri"/><family val="2"/></font>""",
            """<font><b/><sz val="14"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>""",
            """<font><i/><sz val="9"/><color rgb="FF475569"/><name val="Calibri"/><family val="2"/></font>""",
            """<font><b/><sz val="9.5"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>""",
            """<font><b/><sz val="10"/><color rgb="FF0F172A"/><name val="Calibri"/><family val="2"/></font>""",
            """<font><b/><sz val="12"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>""",
            """<font><sz val="9"/><color rgb="FF64748B"/><name val="Calibri"/><family val="2"/></font>"""
        )
        // Rellenos: 0 none, 1 gray125 (obligatorios), 2 azul marino, 3 verde esmeralda, 4 amarillo editable, 5 gris total, 6 gris claro auxiliar
        val fills = listOf(
            """<fill><patternFill patternType="none"/></fill>""", """<fill><patternFill patternType="gray125"/></fill>""",
            solido("FF13294B"), solido("FF0F8B6D"), solido("FFFFF8C5"), solido("FFE2E8F0"), solido("FFF1F5F9")
        )
        val borde = """<border><left style="thin"><color rgb="FFCBD5E1"/></left><right style="thin"><color rgb="FFCBD5E1"/></right><top style="thin"><color rgb="FFCBD5E1"/></top><bottom style="thin"><color rgb="FFCBD5E1"/></bottom><diagonal/></border>"""
        val firma = """<border><left/><right/><top style="thin"><color rgb="FF0F172A"/></top><bottom/><diagonal/></border>"""
        val borders = listOf("""<border><left/><right/><top/><bottom/><diagonal/></border>""", borde, firma)
        // xf(numFmt, font, fill, border, alineación)
        fun xf(fmt: Int, font: Int, fill: Int, border: Int, h: String? = null, wrap: Boolean = false, v: String = "center") =
            """<xf numFmtId="$fmt" fontId="$font" fillId="$fill" borderId="$border" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment${h?.let { " horizontal=\"$it\"" } ?: ""} vertical="$v"${if (wrap) " wrapText=\"1\"" else ""}/></xf>"""
        val xfs = listOf(
            xf(0, 0, 0, 0),                              // 0 NORMAL
            xf(0, 1, 2, 0, "center", true),              // 1 TITULO
            xf(0, 2, 0, 0, "left", true),                // 2 NOTA
            xf(0, 3, 3, 1, "center", true),              // 3 ENCABEZADO
            xf(49, 0, 0, 1, "left", true),               // 4 TEXTO (formato texto: conserva ceros a la izquierda)
            xf(164, 0, 0, 1, "right"),                   // 5 DINERO
            xf(49, 0, 4, 1, "left", true),               // 6 EDITABLE_TEXTO
            xf(164, 0, 4, 1, "right"),                   // 7 EDITABLE_DINERO
            xf(165, 0, 4, 1, "right"),                   // 8 EDITABLE_NUMERO
            xf(165, 0, 0, 1, "right"),                   // 9 NUMERO
            xf(0, 4, 5, 1, "left", true),                // 10 TOTAL_TEXTO
            xf(164, 4, 5, 1, "right"),                   // 11 TOTAL_DINERO
            xf(166, 0, 4, 1, "right"),                   // 12 EDITABLE_PORCENTAJE
            xf(1, 0, 0, 1, "center"),                    // 13 ENTERO
            xf(164, 5, 3, 1, "right"),                   // 14 NETO
            xf(0, 3, 2, 1, "center", true),              // 15 SECCION
            xf(0, 0, 0, 2, "center", true, "top"),       // 16 FIRMA
            xf(0, 4, 0, 0, "left"),                      // 17 NEGRITA
            xf(1, 0, 4, 1, "center"),                    // 18 EDITABLE_ENTERO
            xf(165, 6, 6, 1, "right"),                   // 19 AUXILIAR
            xf(165, 4, 5, 1, "right"),                   // 20 TOTAL_NUMERO
            xf(166, 0, 0, 1, "right"),                   // 21 PORCENTAJE
            xf(0, 5, 3, 1, "left", true)                 // 22 NETO_TEXTO
        )
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<numFmts count="3"><numFmt numFmtId="164" formatCode="$fmtDinero"/><numFmt numFmtId="165" formatCode="#,##0.00"/><numFmt numFmtId="166" formatCode="0.00%"/></numFmts>""")
            append("""<fonts count="${fonts.size}">${fonts.joinToString("")}</fonts>""")
            append("""<fills count="${fills.size}">${fills.joinToString("")}</fills>""")
            append("""<borders count="${borders.size}">${borders.joinToString("")}</borders>""")
            append("""<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""")
            append("""<cellXfs count="${xfs.size}">${xfs.joinToString("")}</cellXfs>""")
            append("""<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>""")
        }
    }

    private fun solido(rgb: String) = """<fill><patternFill patternType="solid"><fgColor rgb="$rgb"/><bgColor indexed="64"/></patternFill></fill>"""
}
