package com.juanreyna.registrotiempo.util

import com.juanreyna.registrotiempo.domain.calc.Boleta
import com.juanreyna.registrotiempo.domain.calc.ParametrosLegales
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.calc.ResumenMensual
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.SistemaPension
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.Xlsx.Estilo
import java.io.OutputStream
import kotlin.math.roundToLong

/**
 * Libro Excel de planilla del Modo Avanzado Perú con fórmulas vivas, en tres hojas:
 * 1. Parametros_Legales — RMV, UIT, tasas de ONP/AFP, EsSalud, SCTR, Vida Ley, factores de beneficios y tramos de 5.ª.
 *    Las celdas amarillas se pueden editar y recalculan todo el libro.
 * 2. Planilla_Mensual — una fila por colaborador: datos, asistencia, ingresos, descuentos, neto, aportes, provisiones y costo.
 *    Las fórmulas replican el cálculo de la app (PlanillaPeruCalculator); los valores iniciales son los de la app.
 * 3. Boleta_Pago_Individual — se elige el N° de documento en la lista desplegable y la boleta se llena sola (DS 001-98-TR).
 */
object PlanillaExcel {

    const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private const val HP = "Parametros_Legales"
    private const val HM = "Planilla_Mensual"
    private const val HB = "Boleta_Pago_Individual"
    private const val P = "$HP!"
    private const val PRIMERA = 6

    // ---------- Filas de la hoja de parámetros (referenciadas por las fórmulas) ----------
    private const val F_RMV = 5; private const val F_AF = 6; private const val F_UIT = 7; private const val F_DIAS = 8
    private const val F_JORNADA = 9; private const val F_HORAS_MES = 10; private const val F_HE25 = 11; private const val F_HE35 = 12
    private const val F_NOCTURNO = 13; private const val F_ONP = 14; private const val F_AFP = 15; private const val F_PRIMA = 16
    private const val F_RMA = 17; private const val F_INTEGRA = 18; private const val F_PRIMA_AFP = 19; private const val F_PROFUTURO = 20
    private const val F_HABITAT = 21; private const val F_ESSALUD = 22; private const val F_SCTR = 23; private const val F_VIDA = 24
    private const val F_BONIF = 25; private const val F_GENERAL = 26; private const val F_PEQUENA = 27; private const val F_MICRO = 28
    private const val F_DEDUCCION = 29; private const val F_TRAMO1 = 32; private const val F_TRAMO5 = 36

    private fun p(fila: Int) = "$P\$B\$$fila"

    /** Tasas referenciales que la app no calcula (dependen de la aseguradora y la actividad): editables en el libro. */
    const val VIDA_LEY_REFERENCIAL = 0.0053
    const val SCTR_REFERENCIAL = 0.0

    // ---------- Columnas de la planilla ----------
    private val COLS = listOf(
        // 1. Datos del colaborador
        "A" to "N°", "B" to "Tipo Doc.", "C" to "N° Documento", "D" to "Apellidos y Nombres", "E" to "Cargo / Puesto",
        "F" to "Centro de Costo", "G" to "Fecha Ingreso", "H" to "Régimen Laboral", "I" to "Tipo Remuneración",
        "J" to "Sistema Pensión", "K" to "CUSPP", "L" to "Comisión Mixta",
        // 2. Asistencia
        "M" to "Días Comput.", "N" to "Días Trab.", "O" to "Faltas", "P" to "Min. Tardanza", "Q" to "Horas Ordinarias",
        "R" to "Horas Ext. 25%", "S" to "Horas Ext. 35%", "T" to "Días Pagados",
        // 3. Ingresos
        "U" to "Sueldo Mensual", "V" to "Tarifa por Hora", "W" to "Asig. Fam. (Sí/No)", "X" to "Remuneración Básica",
        "Y" to "Asig. Familiar", "Z" to "Valor Hora Extra", "AA" to "Importe H.E. 25%", "AB" to "Importe H.E. 35%",
        "AC" to "(−) Tardanzas", "AD" to "Bonos / Otros Afectos", "AE" to "TOTAL INGRESOS",
        // 4. Descuentos
        "AF" to "ONP (13%)", "AG" to "AFP Fondo (10%)", "AH" to "AFP Prima Seguro", "AI" to "AFP Comisión",
        "AJ" to "Renta 5ta Cat.", "AK" to "Adelantos / Otros", "AL" to "TOTAL DESCUENTOS",
        // 5. Neto
        "AM" to "NETO A PAGAR",
        // 6. Aportes
        "AN" to "EsSalud (9%)", "AO" to "SCTR", "AP" to "Seguro Vida Ley", "AQ" to "TOTAL APORTES",
        // 7. Provisiones
        "AR" to "Gratificación", "AS" to "Bonif. Extra. (9%)", "AT" to "CTS", "AU" to "Vacaciones", "AV" to "TOTAL PROVISIONES",
        // Costo y auxiliares
        "AW" to "COSTO TOTAL EMPRESA", "AX" to "Rem. Computable", "AY" to "Proyección Anual 5ta", "AZ" to "Factor Beneficios",
        "BA" to "Observaciones"
    )
    private val ANCHOS = mapOf(
        "A" to 5.0, "B" to 7.0, "C" to 12.0, "D" to 32.0, "E" to 22.0, "F" to 12.0, "G" to 11.0, "H" to 15.0, "I" to 13.0,
        "J" to 13.0, "K" to 13.0, "L" to 9.0, "AE" to 15.0, "AL" to 15.0, "AM" to 17.0, "AQ" to 14.0, "AV" to 15.0, "AW" to 17.0, "BA" to 60.0
    )
    private val SECCIONES = listOf(
        "A4:L4" to "1. DATOS DEL COLABORADOR Y VÍNCULO", "M4:T4" to "2. ASISTENCIA (desde los marcajes)",
        "U4:AE4" to "3. INGRESOS DEVENGADOS", "AF4:AL4" to "4. DESCUENTOS AL TRABAJADOR", "AM4:AM4" to "5. NETO",
        "AN4:AQ4" to "6. APORTES DEL EMPLEADOR", "AR4:AV4" to "7. PROVISIONES (BENEFICIOS SOCIALES)",
        "AW4:AW4" to "COSTO", "AX4:BA4" to "CÁLCULO AUXILIAR"
    )
    /** Columnas numéricas que se suman en la fila de totales. */
    private val SUMABLES = listOf("M", "N", "O", "P", "Q", "R", "S", "T", "X", "Y", "AA", "AB", "AC", "AD", "AE", "AF", "AG", "AH",
        "AI", "AJ", "AK", "AL", "AM", "AN", "AO", "AP", "AQ", "AR", "AS", "AT", "AU", "AV", "AW")

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

    /** Documento que identifica la fila (y que se elige en la boleta). */
    private fun documento(u: Usuario, i: Int) = u.dni.ifBlank { "SIN-DOC-${i + 1}" }

    // =====================================================================================
    /**
     * @param soloDocumento si se indica, la boleta queda abierta en ese colaborador (descarga individual).
     * @param incluirPlanilla false = solo la hoja de parámetros legales.
     */
    fun escribir(
        out: OutputStream,
        empresa: Empresa,
        resumenes: List<ResumenColaborador>,
        mes: String,
        p: ParametrosLegales = ParametrosLegales.delMes(mes),
        incluirPlanilla: Boolean = true,
        soloDocumento: String? = null,
        generadoEl: String = Fechas.hoy()
    ) {
        val filas = resumenes.filter { it.usuario.configPago.modo == ModoPago.AVANZADO }
        val hojas = mutableListOf(hojaParametros(empresa, mes, p))
        if (incluirPlanilla) {
            require(filas.isNotEmpty()) { "no hay colaboradores en Modo Avanzado en este periodo" }
            hojas += hojaPlanilla(empresa, filas, mes, p, generadoEl)
            hojas += hojaBoleta(empresa, filas, mes, soloDocumento ?: documento(filas.first().usuario, 0))
        }
        Xlsx.escribir(out, hojas, empresa.moneda.simbolo)
    }

    // ---------- Hoja 1: parámetros legales ----------
    private fun hojaParametros(empresa: Empresa, mes: String, p: ParametrosLegales): Xlsx.Hoja = Xlsx.Hoja(HP).apply {
        cuadricula = false
        ancho("A", 46.0); ancho("B", 14.0); ancho("C", 9.0); ancho("D", 58.0); ancho("E", 11.0); ancho("F", 11.0)
        texto("A1", "PARÁMETROS LEGALES, LABORALES Y TRIBUTARIOS — PERÚ", Estilo.TITULO); combinar("A1:F1", Estilo.TITULO); alto(1, 28.0)
        texto("A2", "Periodo: ${Fechas.etiquetaMes(mes).uppercase()}   ·   Empresa: ${empresa.razonSocial.ifBlank { empresa.nombreComercial }}" +
            (if (empresa.ruc.isNotBlank()) "   ·   RUC ${empresa.ruc}" else ""), Estilo.NEGRITA)
        combinar("A2:F2")
        texto("A3", "Las celdas amarillas son editables y recalculan automáticamente la Planilla Mensual y la Boleta. " +
            "Revise estos valores cada año (RMV, UIT, tasas AFP y Vida Ley).", Estilo.NOTA)
        combinar("A3:F3"); alto(3, 26.0)
        listOf("A4" to "Parámetro / Concepto", "B4" to "Valor / Tasa", "C4" to "Unidad", "D4" to "Base legal / Observación").forEach { (r, t) -> texto(r, t, Estilo.ENCABEZADO) }
        combinar("D4:F4", Estilo.ENCABEZADO); alto(4, 22.0)

        val rmvBase = if (p.rmv >= 1230.0) "D.S. N° 015-2026-TR (vigente desde el 01/10/2026)" else "D.S. N° 006-2024-TR (vigente desde el 01/01/2025)"
        val uitBase = if (p.uit >= 5500.0) "D.S. N° 301-2025-EF (UIT 2026)" else "D.S. N° 260-2024-EF (UIT 2025)"
        data class Par(val fila: Int, val concepto: String, val valor: Double, val unidad: String, val base: String, val estilo: Estilo)
        val pct = Estilo.EDITABLE_PORCENTAJE
        val ed = Estilo.EDITABLE_NUMERO
        listOf(
            Par(F_RMV, "Remuneración Mínima Vital (RMV)", p.rmv, "S/", rmvBase, Estilo.EDITABLE_DINERO),
            Par(F_AF, "Asignación familiar (% de la RMV)", 0.10, "%", "Ley N° 25129 — 10 % de la RMV por hijos menores o en estudios superiores", pct),
            Par(F_UIT, "Unidad Impositiva Tributaria (UIT)", p.uit, "S/", uitBase, Estilo.EDITABLE_DINERO),
            Par(F_DIAS, "Días del mes para la remuneración", p.diasMes.toDouble(), "Días", "Mes comercial de 30 días (incluye descansos semanales)", ed),
            Par(F_JORNADA, "Horas de la jornada diaria ordinaria", p.jornadaDiariaBase.toDouble(), "Horas", "Jornada máxima: 8 h diarias o 48 h semanales (Const. art. 25; D.S. N° 007-2002-TR)", ed),
            Par(F_HORAS_MES, "Horas mensuales base (valor hora)", p.horasMensualesBase.toDouble(), "Horas", "= días × horas diarias: valor hora = remuneración ÷ 240", ed),
            Par(F_HE25, "Sobretasa hora extra — 2 primeras horas del día", 1.25, "Factor", "Mínimo 25 % (D.S. N° 007-2002-TR, art. 10)", ed),
            Par(F_HE35, "Sobretasa hora extra — desde la 3.ª hora", 1.35, "Factor", "Mínimo 35 % (D.S. N° 007-2002-TR, art. 10)", ed),
            Par(F_NOCTURNO, "Trabajo nocturno (10 p. m. a 6 a. m.)", 0.35, "%", "Remuneración no menor a la RMV + 35 % (art. 8). Referencial: no se aplica automáticamente", pct),
            Par(F_ONP, "ONP — Sistema Nacional de Pensiones", p.tasaOnp, "%", "D.L. N° 19990 — 13 % de la remuneración", pct),
            Par(F_AFP, "AFP — Aporte obligatorio al fondo", p.aporteAfp, "%", "T.U.O. Ley del SPP (D.S. N° 054-97-EF) — 10 %", pct),
            Par(F_PRIMA, "AFP — Prima de seguro (invalidez, sobrevivencia y sepelio)", p.primaSeguroAfp, "%", "Tasa vigente SBS; se aplica hasta la remuneración máxima asegurable", pct),
            Par(F_RMA, "AFP — Remuneración máxima asegurable (tope de la prima)", p.remuneracionMaximaAsegurable, "S/", "Tope trimestral publicado por la SBS (editable)", Estilo.EDITABLE_DINERO),
            Par(F_INTEGRA, "AFP Integra — Comisión sobre flujo", p.comisionFlujo[SistemaPension.AFP_INTEGRA] ?: 0.0, "%", "0 % si el afiliado está en comisión mixta (sobre saldo)", pct),
            Par(F_PRIMA_AFP, "AFP Prima — Comisión sobre flujo", p.comisionFlujo[SistemaPension.AFP_PRIMA] ?: 0.0, "%", "0 % si el afiliado está en comisión mixta (sobre saldo)", pct),
            Par(F_PROFUTURO, "AFP Profuturo — Comisión sobre flujo", p.comisionFlujo[SistemaPension.AFP_PROFUTURO] ?: 0.0, "%", "0 % si el afiliado está en comisión mixta (sobre saldo)", pct),
            Par(F_HABITAT, "AFP Hábitat — Comisión sobre flujo", p.comisionFlujo[SistemaPension.AFP_HABITAT] ?: 0.0, "%", "0 % si el afiliado está en comisión mixta (sobre saldo)", pct),
            Par(F_ESSALUD, "EsSalud — Aporte del empleador", p.tasaEssalud, "%", "Ley N° 26790 — 9 %; base mínima: una RMV. La microempresa REMYPE afilia al SIS", pct),
            Par(F_SCTR, "SCTR Salud + Pensión — Aporte del empleador", SCTR_REFERENCIAL, "%", "Solo actividades de riesgo (Anexo 5, D.S. N° 009-97-SA). Ingrese la tasa de su póliza", pct),
            Par(F_VIDA, "Seguro Vida Ley — Prima del empleador", VIDA_LEY_REFERENCIAL, "%", "D. Leg. N° 688 (D.U. N° 044-2019): desde el primer día. Ingrese la tasa de su póliza", pct),
            Par(F_BONIF, "Bonificación extraordinaria sobre la gratificación", p.bonificacionExtraordinaria, "%", "Ley N° 30334 — monto del aporte a EsSalud (9 %) que el empleador no paga", pct),
            Par(F_GENERAL, "Factor de beneficios — Régimen General", 1.0, "Factor", "Gratificaciones completas (Ley N° 27735), CTS (D.S. N° 001-97-TR), 30 días de vacaciones", ed),
            Par(F_PEQUENA, "Factor de beneficios — REMYPE Pequeña Empresa", 0.5, "Factor", "Media gratificación, CTS de 15 remuneraciones diarias, 15 días de vacaciones (D.S. N° 013-2013-PRODUCE)", ed),
            Par(F_MICRO, "Factor de beneficios — REMYPE Microempresa", 0.0, "Factor", "Sin gratificaciones ni CTS; 15 días de vacaciones", ed),
            Par(F_DEDUCCION, "Renta de 5.ª categoría — deducción anual", p.deduccionQuintaUit, "UIT", "Art. 46 de la Ley del Impuesto a la Renta — 7 UIT", ed)
        ).forEach { x ->
            texto("A${x.fila}", x.concepto); numero("B${x.fila}", x.valor, x.estilo); texto("C${x.fila}", x.unidad); texto("D${x.fila}", x.base)
            combinar("D${x.fila}:F${x.fila}", Estilo.TEXTO)
        }
        // Tramos de la renta de 5.ª categoría (art. 53 LIR): se usan con SUMPRODUCT (tasa marginal adicional)
        texto("A31", "Renta de 5.ª categoría — tramos (renta neta anual)", Estilo.ENCABEZADO)
        texto("B31", "Tasa", Estilo.ENCABEZADO); texto("C31", "Unidad", Estilo.ENCABEZADO)
        texto("D31", "Base legal", Estilo.ENCABEZADO); texto("E31", "Desde (UIT)", Estilo.ENCABEZADO); texto("F31", "Tasa marginal", Estilo.ENCABEZADO)
        alto(31, 22.0)
        val limites = listOf(0.0) + p.tramosQuinta.dropLast(1).map { it.first }
        p.tramosQuinta.forEachIndexed { i, (hasta, tasa) ->
            val f = F_TRAMO1 + i
            val desde = limites[i]
            texto("A$f", if (hasta == Double.MAX_VALUE) "Más de ${fmt(desde)} UIT" else "Más de ${fmt(desde)} hasta ${fmt(hasta)} UIT")
            numero("B$f", tasa, Estilo.EDITABLE_PORCENTAJE); texto("C$f", "%"); texto("D$f", "Art. 53 de la Ley del Impuesto a la Renta")
            numero("E$f", desde, Estilo.EDITABLE_NUMERO)
            formula("F$f", if (i == 0) "B$f" else "B$f-B${f - 1}", tasa - (if (i == 0) 0.0 else p.tramosQuinta[i - 1].second), Estilo.PORCENTAJE)
        }
        texto("A38", "Notas", Estilo.NEGRITA)
        listOf(
            "• Valor hora extra = (sueldo mensual + asignación familiar) ÷ 240; si la remuneración es por hora: tarifa + asignación familiar ÷ 240.",
            "• Remuneración por hora: se paga cada hora trabajada; con menos de 4 h diarias en promedio (tiempo parcial) no corresponden CTS ni vacaciones.",
            "• La retención de 5.ª categoría es una proyección (anual ÷ 12). En la planilla oficial se aplica el procedimiento del art. 40 del Reglamento de la LIR.",
            "• Gratificación, bonificación extraordinaria, CTS y vacaciones son provisiones mensuales del empleador: no se pagan en el mes.",
            "• Documento generado por la app Registro de Tiempo de Personal. Es referencial: verifique con su contador antes de declarar en el PLAME."
        ).forEachIndexed { i, t -> texto("A${39 + i}", t, Estilo.NOTA); combinar("A${39 + i}:F${39 + i}") }
    }

    private fun fmt(v: Double) = if (v == v.roundToLong().toDouble()) v.roundToLong().toString() else v.toString()

    // ---------- Hoja 2: planilla mensual ----------
    /** Valores de una fila tal como los calcula la app (se guardan como caché de las fórmulas). */
    private class Fila(val r: ResumenColaborador, val p: ParametrosLegales) {
        val u = r.usuario; val c = u.configPago; val b: Boleta = r.boleta
        val porHora = c.pagoPorHora
        fun linea(prefijo: String) = (b.ingresos + b.descuentos + b.aportesEmpleador + b.provisiones)
            .firstOrNull { it.concepto.startsWith(prefijo) || it.concepto.contains(prefijo) }?.monto ?: 0.0
        val diasComputables = b.diasComputables
        val diasTrab = if (porHora) b.diasLaborados ?: 0 else r.dias.count { it.minutosEfectivos > 0 }
        val basica = linea("Remuneración"); val af = linea("Asignación familiar")
        val he25 = linea("Horas extras 25"); val he35 = linea("Horas extras 35"); val tard = linea("(−) Tardanzas")
        val ingresos = (basica + af + he25 + he35 + tard).coerceAtLeast(0.0)
        val vhExtra = (if (porHora) c.tarifaPorHora else c.sueldoBase / p.horasMensualesBase) + af / p.horasMensualesBase
        val onp = linea("ONP"); val fondo = linea("Aporte obligatorio"); val prima = linea("Prima de seguro")
        val comision = linea("Comisión sobre flujo"); val quinta = linea("Renta de 5.ª")
        val descuentos = onp + fondo + prima + comision + quinta
        val essalud = linea("EsSalud")
        val sctr = r2(ingresos * SCTR_REFERENCIAL); val vida = r2(ingresos * VIDA_LEY_REFERENCIAL)
        val grati = linea("Provisión gratificación"); val bonif = linea("Provisión bonificación")
        val cts = linea("Provisión CTS"); val vac = linea("Provisión vacaciones")
        val remComputable = if (porHora) basica + af else c.sueldoBase + af
        val factor = when (c.regimen) { RegimenLaboral.GENERAL -> 1.0; RegimenLaboral.PEQUENA_EMPRESA -> 0.5; RegimenLaboral.MICROEMPRESA -> 0.0 }
        val proyeccion = remComputable * 12 + remComputable * factor * 2 * (1 + p.bonificacionExtraordinaria) + he25 + he35
    }

    private fun r2(x: Double) = java.math.BigDecimal.valueOf(x).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()

    private fun hojaPlanilla(empresa: Empresa, resumenes: List<ResumenColaborador>, mes: String, p: ParametrosLegales, generadoEl: String) =
        Xlsx.Hoja(HM).apply {
            horizontal = true
            inmovilizar = 4 to 5
            COLS.forEach { (col, _) -> ancho(col, ANCHOS[col] ?: 12.5) }
            val ultima = COLS.last().first
            texto("A1", "PLANILLA DE REMUNERACIONES MENSUAL — MODO AVANZADO PERÚ (BASE PARA PLAME)", Estilo.TITULO)
            combinar("A1:${ultima}1", Estilo.TITULO); alto(1, 30.0)
            texto("A2", "RAZÓN SOCIAL: ${empresa.razonSocial.ifBlank { empresa.nombreComercial }.uppercase()}   |   RUC: ${empresa.ruc.ifBlank { "— (registrar en la app: Reportes → Datos del empleador)" }}", Estilo.NEGRITA)
            combinar("A2:H2")
            texto("I2", "PERIODO: ${Fechas.etiquetaMes(mes).uppercase()}   |   MONEDA: ${if (empresa.moneda.simbolo == "S/") "SOLES (S/)" else "DÓLARES (US$)"}", Estilo.NEGRITA)
            combinar("I2:T2")
            texto("U2", "Generado el ${fechaLegible(generadoEl)} con la app Registro de Tiempo de Personal · Proyección con los marcajes a la fecha · Celdas amarillas editables", Estilo.NOTA)
            combinar("U2:${ultima}2"); alto(2, 20.0)
            SECCIONES.forEach { (rango, t) ->
                texto(rango.substringBefore(":"), t, Estilo.SECCION)
                if (rango.substringBefore(":") != rango.substringAfter(":")) combinar(rango, Estilo.SECCION)
            }
            alto(4, 22.0)
            COLS.forEach { (col, t) -> texto("${col}5", t, Estilo.ENCABEZADO) }
            alto(5, 32.0)

            val fin = PRIMERA + resumenes.size - 1
            resumenes.forEachIndexed { i, r ->
                val x = Fila(r, p)
                val n = PRIMERA + i
                val u = x.u; val c = x.c
                fun f(col: String) = "$col$n"
                numero(f("A"), (i + 1).toDouble(), Estilo.ENTERO)
                texto(f("B"), u.tipoDocumento.etiqueta)
                texto(f("C"), documento(u, i))
                texto(f("D"), nombreLegal(u))
                texto(f("E"), c.cargo.ifBlank { "—" })
                texto(f("F"), c.centroCosto.ifBlank { "—" })
                texto(f("G"), ResumenMensual.fechaIngreso(u)?.let { fechaLegible(it) } ?: "—")
                texto(f("H"), etiquetaRegimen(c.regimen), Estilo.EDITABLE_TEXTO)
                texto(f("I"), if (x.porHora) "Por hora" else "Mensual", Estilo.EDITABLE_TEXTO)
                texto(f("J"), c.sistemaPension.etiqueta, Estilo.EDITABLE_TEXTO)
                texto(f("K"), if (c.sistemaPension == SistemaPension.ONP) "—" else c.cuspp.ifBlank { "—" })
                texto(f("L"), if (c.comisionMixta && c.sistemaPension != SistemaPension.ONP) "Sí" else "No", Estilo.EDITABLE_TEXTO)
                // Asistencia (editable: viene de los marcajes)
                numero(f("M"), x.diasComputables.toDouble(), Estilo.EDITABLE_ENTERO)
                numero(f("N"), x.diasTrab.toDouble(), Estilo.EDITABLE_ENTERO)
                numero(f("O"), x.b.faltas.toDouble(), Estilo.EDITABLE_ENTERO)
                numero(f("P"), x.b.minutosTardanza.toDouble(), Estilo.EDITABLE_ENTERO)
                numero(f("Q"), x.b.horasOrdinarias, Estilo.EDITABLE_NUMERO)
                numero(f("R"), x.b.horasExtra25, Estilo.EDITABLE_NUMERO)
                numero(f("S"), x.b.horasExtra35, Estilo.EDITABLE_NUMERO)
                formula(f("T"), "IF(I$n=\"Por hora\",N$n,MAX(0,M$n-MIN(O$n,M$n)))", (x.b.diasLaborados ?: 0).toDouble(), Estilo.ENTERO)
                // Ingresos
                numero(f("U"), c.sueldoBase, Estilo.EDITABLE_DINERO)
                numero(f("V"), if (x.porHora) c.tarifaPorHora else 0.0, Estilo.EDITABLE_DINERO)
                texto(f("W"), if (c.asignacionFamiliar) "Sí" else "No", Estilo.EDITABLE_TEXTO)
                formula(f("X"), "IF(I$n=\"Por hora\",ROUND(Q$n*V$n,2),ROUND(U$n/${p(F_DIAS)}*T$n,2))", x.basica)
                formula(f("Y"), "IF(W$n=\"Sí\",ROUND(${p(F_RMV)}*${p(F_AF)},2),0)", x.af)
                formula(f("Z"), "IF(I$n=\"Por hora\",V$n,U$n/${p(F_HORAS_MES)})+Y$n/${p(F_HORAS_MES)}", x.vhExtra, Estilo.AUXILIAR)
                formula(f("AA"), "ROUND(R$n*Z$n*${p(F_HE25)},2)", x.he25)
                formula(f("AB"), "ROUND(S$n*Z$n*${p(F_HE35)},2)", x.he35)
                formula(f("AC"), "IF(I$n=\"Por hora\",0,-ROUND(P$n/60*U$n/${p(F_HORAS_MES)},2))", x.tard)
                numero(f("AD"), 0.0, Estilo.EDITABLE_DINERO)
                formula(f("AE"), "MAX(0,X$n+Y$n+AA$n+AB$n+AC$n+AD$n)", x.ingresos, Estilo.TOTAL_DINERO)
                // Descuentos
                formula(f("AF"), "IF(J$n=\"ONP\",ROUND(AE$n*${p(F_ONP)},2),0)", x.onp)
                formula(f("AG"), "IF(J$n=\"ONP\",0,ROUND(AE$n*${p(F_AFP)},2))", x.fondo)
                formula(f("AH"), "IF(J$n=\"ONP\",0,ROUND(MIN(AE$n,${p(F_RMA)})*${p(F_PRIMA)},2))", x.prima)
                formula(
                    f("AI"),
                    "IF(OR(J$n=\"ONP\",L$n=\"Sí\"),0,ROUND(AE$n*IF(J$n=\"AFP Integra\",${p(F_INTEGRA)},IF(J$n=\"AFP Prima\",${p(F_PRIMA_AFP)}," +
                        "IF(J$n=\"AFP Profuturo\",${p(F_PROFUTURO)},IF(J$n=\"AFP Hábitat\",${p(F_HABITAT)},0)))),2))",
                    x.comision
                )
                val neta = "MAX(0,AY$n-${p(F_DEDUCCION)}*${p(F_UIT)})"
                val desde = "$P\$E\$$F_TRAMO1:\$E\$$F_TRAMO5*${p(F_UIT)}"
                formula(f("AJ"), "ROUND(ROUND(SUMPRODUCT(($neta>$desde)*($neta-$desde)*$P\$F\$$F_TRAMO1:\$F\$$F_TRAMO5),2)/12,2)", x.quinta)
                numero(f("AK"), 0.0, Estilo.EDITABLE_DINERO)
                formula(f("AL"), "SUM(AF$n:AK$n)", x.descuentos, Estilo.TOTAL_DINERO)
                formula(f("AM"), "AE$n-AL$n", r2(x.ingresos - x.descuentos), Estilo.NETO)
                // Aportes del empleador
                formula(
                    f("AN"),
                    "IF(H$n=\"Microempresa\",0,ROUND(MAX(AE$n,IF(AND(I$n=\"Por hora\",AE$n<=0),0,${p(F_RMV)}*M$n/${p(F_DIAS)}))*${p(F_ESSALUD)},2))",
                    x.essalud
                )
                formula(f("AO"), "ROUND(AE$n*${p(F_SCTR)},2)", x.sctr)
                formula(f("AP"), "ROUND(AE$n*${p(F_VIDA)},2)", x.vida)
                formula(f("AQ"), "SUM(AN$n:AP$n)", r2(x.essalud + x.sctr + x.vida), Estilo.TOTAL_DINERO)
                // Provisiones
                val parcial = "AND(I$n=\"Por hora\",N$n>0,(Q$n+R$n+S$n)/MAX(1,N$n)<4)"
                val fraccion = "IF(I$n=\"Por hora\",1,T$n/${p(F_DIAS)})"
                formula(f("AR"), "IF(OR(I$n=\"Por hora\",M$n=${p(F_DIAS)}),ROUND(AX$n*AZ$n/6,2),0)", x.grati)
                formula(f("AS"), "ROUND(AR$n*${p(F_BONIF)},2)", x.bonif)
                formula(f("AT"), "IF($parcial,0,ROUND(AZ$n*(AX$n+AX$n*AZ$n/6)/12*$fraccion,2))", x.cts)
                formula(f("AU"), "IF($parcial,0,ROUND(AX$n/IF(H$n=\"General\",12,24)*$fraccion,2))", x.vac)
                formula(f("AV"), "SUM(AR$n:AU$n)", r2(x.grati + x.bonif + x.cts + x.vac), Estilo.TOTAL_DINERO)
                formula(f("AW"), "AE$n+AQ$n+AV$n", r2(x.ingresos + x.essalud + x.sctr + x.vida + x.grati + x.bonif + x.cts + x.vac), Estilo.TOTAL_DINERO)
                // Auxiliares
                formula(f("AX"), "IF(I$n=\"Por hora\",X$n+Y$n,U$n+Y$n)", x.remComputable, Estilo.AUXILIAR)
                formula(f("AY"), "AX$n*12+AX$n*AZ$n*2*(1+${p(F_BONIF)})+AA$n+AB$n", x.proyeccion, Estilo.AUXILIAR)
                formula(f("AZ"), "IF(H$n=\"General\",${p(F_GENERAL)},IF(H$n=\"Pequeña Empresa\",${p(F_PEQUENA)},${p(F_MICRO)}))", x.factor, Estilo.AUXILIAR)
                texto(f("BA"), x.b.advertencias.joinToString(" ").ifBlank { "—" })
            }
            // Listas desplegables en los campos editables de texto
            lista("H$PRIMERA:H$fin", "\"General,Pequeña Empresa,Microempresa\"")
            lista("I$PRIMERA:I$fin", "\"Mensual,Por hora\"")
            lista("J$PRIMERA:J$fin", "\"ONP,AFP Integra,AFP Prima,AFP Profuturo,AFP Hábitat\"")
            lista("L$PRIMERA:L$fin", "\"Sí,No\"")
            lista("W$PRIMERA:W$fin", "\"Sí,No\"")

            // Totales
            val t = fin + 1
            texto("A$t", "TOTALES GENERALES DE LA PLANILLA", Estilo.TOTAL_TEXTO); combinar("A$t:L$t", Estilo.TOTAL_TEXTO)
            SUMABLES.forEach { col ->
                val dinero = col >= "U" || col.length == 2
                formula("$col$t", "SUM($col$PRIMERA:$col$fin)", null, if (dinero) Estilo.TOTAL_DINERO else Estilo.TOTAL_NUMERO)
            }
            listOf("U", "V", "W", "Z", "AX", "AY", "AZ", "BA").forEach { vacia("$it$t", Estilo.TOTAL_TEXTO) }
            alto(t, 22.0)

            // Resumen contable y tesorería
            val r0 = t + 2
            texto("C$r0", "RESUMEN CONTABLE Y TESORERÍA", Estilo.SECCION); combinar("C$r0:E$r0", Estilo.SECCION)
            listOf(
                "Total remuneraciones brutas devengadas" to "AE$t",
                "Total descuentos ONP (PLAME)" to "AF$t",
                "Total descuentos AFP (AFPnet)" to "AG$t+AH$t+AI$t",
                "Total retención renta de 5.ª categoría" to "AJ$t",
                "Total descuentos internos / adelantos" to "AK$t",
                "TOTAL NETO A ABONAR EN CUENTAS SUELDO" to "AM$t",
                "Total aportes del empleador (EsSalud + SCTR + Vida Ley)" to "AQ$t",
                "Total provisiones (gratificación, bonificación, CTS y vacaciones)" to "AV$t",
                "COSTO LABORAL TOTAL DEL PERIODO" to "AW$t"
            ).forEachIndexed { i, (txt, f) ->
                val fila = r0 + 1 + i
                val fuerte = txt.startsWith("TOTAL NETO") || txt.startsWith("COSTO")
                texto("C$fila", txt, if (fuerte) Estilo.TOTAL_TEXTO else Estilo.TEXTO); combinar("C$fila:D$fila", if (fuerte) Estilo.TOTAL_TEXTO else Estilo.TEXTO)
                formula("E$fila", f, null, if (fuerte) Estilo.TOTAL_DINERO else Estilo.DINERO)
            }
            // Firmas
            val ff = r0 + 3
            listOf("H" to "J" to "Elaborado por: Recursos Humanos", "L" to "N" to "Revisado por: Contabilidad", "P" to "R" to "Aprobado por: Gerencia General")
                .forEach { (rango, txt) ->
                    texto("${rango.first}$ff", txt, Estilo.FIRMA); combinar("${rango.first}$ff:${rango.second}$ff", Estilo.FIRMA)
                }
        }

    // ---------- Hoja 3: boleta individual ----------
    private fun hojaBoleta(empresa: Empresa, resumenes: List<ResumenColaborador>, mes: String, documentoInicial: String) =
        Xlsx.Hoja(HB).apply {
            cuadricula = false
            listOf("A" to 30.0, "B" to 15.0, "C" to 30.0, "D" to 15.0, "E" to 30.0, "F" to 15.0).forEach { (c, w) -> ancho(c, w) }
            val fin = PRIMERA + resumenes.size - 1
            val inicial = resumenes.indexOfFirst { documento(it.usuario, resumenes.indexOf(it)) == documentoInicial }.coerceAtLeast(0)
            val x = Fila(resumenes[inicial], ParametrosLegales.delMes(mes))
            fun busca(col: String) = "IFERROR(INDEX($HM!\$$col\$$PRIMERA:\$$col\$$fin,MATCH(\$C\$2,$HM!\$C\$$PRIMERA:\$C\$$fin,0)),\"\")"
            fun numBusca(col: String) = "IFERROR(INDEX($HM!\$$col\$$PRIMERA:\$$col\$$fin,MATCH(\$C\$2,$HM!\$C\$$PRIMERA:\$C\$$fin,0)),0)"

            texto("A1", "BOLETA DE PAGO DE REMUNERACIONES (D.S. N° 001-98-TR)", Estilo.TITULO); combinar("A1:F1", Estilo.TITULO); alto(1, 30.0)
            texto("A2", "N° de documento del trabajador:", Estilo.NEGRITA); combinar("A2:B2")
            texto("C2", documentoInicial, Estilo.EDITABLE_TEXTO)
            lista("C2", "$HM!\$C\$$PRIMERA:\$C\$$fin")
            texto("D2", "← Elija el documento en la lista para generar la boleta de otro trabajador", Estilo.NOTA); combinar("D2:F2")

            fun dato(fila: Int, et1: String, f1: String, cache1: String, et2: String, f2: String, cache2: String) {
                texto("A$fila", et1, Estilo.TOTAL_TEXTO); formulaTexto("B$fila", f1, cache1); combinar("B$fila:C$fila", Estilo.TEXTO)
                texto("D$fila", et2, Estilo.TOTAL_TEXTO); formulaTexto("E$fila", f2, cache2); combinar("E$fila:F$fila", Estilo.TEXTO)
            }
            val razon = empresa.razonSocial.ifBlank { empresa.nombreComercial }
            texto("A4", "Empleador (razón social):", Estilo.TOTAL_TEXTO); texto("B4", razon); combinar("B4:C4", Estilo.TEXTO)
            texto("D4", "RUC:", Estilo.TOTAL_TEXTO); texto("E4", empresa.ruc.ifBlank { "—" }); combinar("E4:F4", Estilo.TEXTO)
            texto("A5", "Periodo:", Estilo.TOTAL_TEXTO); texto("B5", Fechas.etiquetaMes(mes)); combinar("B5:C5", Estilo.TEXTO)
            texto("D5", "Moneda:", Estilo.TOTAL_TEXTO); texto("E5", if (empresa.moneda.simbolo == "S/") "Soles (S/)" else "Dólares (US$)"); combinar("E5:F5", Estilo.TEXTO)
            dato(6, "Trabajador:", busca("D"), nombreLegal(x.u), "Tipo y N° de documento:", "${busca("B")}&\" \"&\$C\$2", "${x.u.tipoDocumento.etiqueta} $documentoInicial")
            dato(7, "Cargo / puesto:", busca("E"), x.c.cargo.ifBlank { "—" }, "Fecha de ingreso:", busca("G"), ResumenMensual.fechaIngreso(x.u)?.let { fechaLegible(it) } ?: "—")
            dato(8, "Régimen laboral:", busca("H"), etiquetaRegimen(x.c.regimen), "Tipo de remuneración:", busca("I"), if (x.porHora) "Por hora" else "Mensual")
            dato(9, "Régimen pensionario:", busca("J"), x.c.sistemaPension.etiqueta, "CUSPP:", busca("K"), if (x.c.sistemaPension == SistemaPension.ONP) "—" else x.c.cuspp.ifBlank { "—" })
            dato(10, "Centro de costo:", busca("F"), x.c.centroCosto.ifBlank { "—" }, "Remuneración pactada:",
                "IF(${busca("I")}=\"Por hora\",TEXT(${numBusca("V")},\"#,##0.00\")&\" por hora\",TEXT(${numBusca("U")},\"#,##0.00\")&\" mensual\")",
                if (x.porHora) "%,.2f por hora".format(x.c.tarifaPorHora) else "%,.2f mensual".format(x.c.sueldoBase))
            texto("A11", "Días laborados / faltas:", Estilo.TOTAL_TEXTO)
            formulaTexto("B11", "${numBusca("T")}&\" días · \"&${numBusca("O")}&\" faltas\"", "${x.b.diasLaborados ?: 0} días · ${x.b.faltas} faltas"); combinar("B11:C11", Estilo.TEXTO)
            texto("D11", "Horas ordinarias / extras:", Estilo.TOTAL_TEXTO)
            formulaTexto("E11", "TEXT(${numBusca("Q")},\"0.00\")&\" h · \"&TEXT(${numBusca("R")}+${numBusca("S")},\"0.00\")&\" h extras\"",
                "%.2f h · %.2f h extras".format(x.b.horasOrdinarias, x.b.horasExtra25 + x.b.horasExtra35)); combinar("E11:F11", Estilo.TEXTO)

            listOf("A13" to "INGRESOS", "B13" to "IMPORTE", "C13" to "DESCUENTOS DEL TRABAJADOR", "D13" to "IMPORTE", "E13" to "APORTES DEL EMPLEADOR", "F13" to "IMPORTE")
                .forEach { (r, t) -> texto(r, t, Estilo.ENCABEZADO) }
            alto(13, 22.0)
            data class L(val txt: String, val col: String?, val cache: Double)
            val ing = listOf(
                L("Remuneración básica / proporcional", "X", x.basica), L("Asignación familiar", "Y", x.af),
                L("Horas extras al 25 %", "AA", x.he25), L("Horas extras al 35 %", "AB", x.he35),
                L("(−) Tardanzas no justificadas", "AC", x.tard), L("Bonos / otros ingresos afectos", "AD", 0.0)
            )
            val des = listOf(
                L("ONP (13 %)", "AF", x.onp), L("AFP — Aporte obligatorio (10 %)", "AG", x.fondo), L("AFP — Prima de seguro", "AH", x.prima),
                L("AFP — Comisión sobre flujo", "AI", x.comision), L("Renta de 5.ª categoría", "AJ", x.quinta), L("Adelantos / otros descuentos", "AK", 0.0)
            )
            val apo = listOf(
                L("EsSalud (9 %)", "AN", x.essalud), L("SCTR salud / pensión", "AO", x.sctr), L("Seguro Vida Ley", "AP", x.vida),
                L("", null, 0.0), L("", null, 0.0), L("", null, 0.0)
            )
            for (i in 0 until 6) {
                val f = 14 + i
                listOf(Triple("A", "B", ing[i]), Triple("C", "D", des[i]), Triple("E", "F", apo[i])).forEach { (ct, cv, l) ->
                    texto("$ct$f", l.txt)
                    if (l.col != null) formula("$cv$f", numBusca(l.col), l.cache) else vacia("$cv$f", Estilo.DINERO)
                }
            }
            texto("A20", "TOTAL INGRESOS", Estilo.TOTAL_TEXTO); formula("B20", "SUM(B14:B19)", x.ingresos, Estilo.TOTAL_DINERO)
            texto("C20", "TOTAL DESCUENTOS", Estilo.TOTAL_TEXTO); formula("D20", "SUM(D14:D19)", x.descuentos, Estilo.TOTAL_DINERO)
            texto("E20", "TOTAL APORTES", Estilo.TOTAL_TEXTO); formula("F20", "SUM(F14:F19)", r2(x.essalud + x.sctr + x.vida), Estilo.TOTAL_DINERO)

            texto("A22", "NETO A RECIBIR POR EL TRABAJADOR:", Estilo.NETO_TEXTO); combinar("A22:D22", Estilo.NETO_TEXTO)
            formula("E22", "B20-D20", r2(x.ingresos - x.descuentos), Estilo.NETO); combinar("E22:F22", Estilo.NETO); alto(22, 26.0)

            texto("A24", "PROVISIONES DEL EMPLEADOR (informativo: se pagan en julio, diciembre, mayo/noviembre o al salir de vacaciones)", Estilo.SECCION)
            combinar("A24:F24", Estilo.SECCION); alto(24, 20.0)
            listOf(
                Triple(25, "Gratificación", "AR" to x.grati), Triple(26, "Bonificación extraordinaria (9 %)", "AS" to x.bonif),
                Triple(27, "CTS", "AT" to x.cts), Triple(28, "Vacaciones", "AU" to x.vac)
            ).forEach { (f, t, cv) -> texto("A$f", t); formula("B$f", numBusca(cv.first), cv.second) }
            texto("C25", "Total provisiones", Estilo.TOTAL_TEXTO); formula("D25", "SUM(B25:B28)", r2(x.grati + x.bonif + x.cts + x.vac), Estilo.TOTAL_DINERO)
            texto("C26", "Costo laboral total del empleador", Estilo.TOTAL_TEXTO); formula("D26", "B20+F20+D25", r2(x.ingresos + x.essalud + x.sctr + x.vida + x.grati + x.bonif + x.cts + x.vac), Estilo.TOTAL_DINERO)

            texto("A30", "Observaciones:", Estilo.NEGRITA)
            formulaTexto("B30", busca("BA"), x.b.advertencias.joinToString(" ").ifBlank { "—" }, Estilo.NOTA); combinar("B30:F30"); alto(30, 48.0)
            texto("A32", "Fecha de entrega: ____ / ____ / ________", Estilo.NORMAL)
            texto("A35", "Firma y sello del empleador o representante", Estilo.FIRMA); combinar("A35:B35", Estilo.FIRMA)
            texto("D35", "Recibí conforme — firma y huella digital del trabajador", Estilo.FIRMA); combinar("D35:F35", Estilo.FIRMA)
            texto("A37", "Boleta emitida conforme al D.S. N° 001-98-TR. Puede entregarse en físico o por medios electrónicos con firma digital (D.S. N° 009-2011-TR). " +
                "Documento referencial generado con los marcajes registrados en la app.", Estilo.NOTA)
            combinar("A37:F37"); alto(37, 26.0)
        }

    private fun fechaLegible(iso: String) = iso.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}/${it[0]}" else iso }
}
