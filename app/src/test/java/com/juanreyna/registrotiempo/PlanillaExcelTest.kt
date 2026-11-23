package com.juanreyna.registrotiempo

import com.juanreyna.registrotiempo.domain.calc.HorasDia
import com.juanreyna.registrotiempo.domain.calc.ParametrosLegales
import com.juanreyna.registrotiempo.domain.calc.PeriodoPlanilla
import com.juanreyna.registrotiempo.domain.calc.PlanillaPeruCalculator
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.SistemaPension
import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.PlanillaExcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Genera un libro de prueba con casos variados (mensual, por hora, REMYPE, AFP con comisión mixta, tope RMA, 5.ª categoría).
 * El archivo queda en app/build/planilla_prueba.xlsx para recalcularlo en Excel y comparar con los valores de la app.
 */
class PlanillaExcelTest {

    private val mes = "2026-11"
    private val p = ParametrosLegales.delMes(mes)
    private val empresa = Empresa(nombreComercial = "Distribuidora Ejemplo", razonSocial = "EMPRESA EJEMPLO S.A.C.", ruc = "20601234567")

    private fun caso(
        i: Int, cfg: ConfigPago, dias: List<HorasDia>, periodo: PeriodoPlanilla = PeriodoPlanilla(),
        doc: String = "4589123$i", tipo: TipoDocumento = TipoDocumento.DNI
    ): ResumenColaborador {
        val u = Usuario(uid = "u$i", nombre = "Cuenta $i", dni = doc, tipoDocumento = tipo, configPago = cfg.copy(modo = ModoPago.AVANZADO))
        return ResumenColaborador(u, emptyList(), PlanillaPeruCalculator.calcularDias(u.configPago, dias, p, periodo))
    }

    private fun d(h: Double, extra: Double = 0.0) = HorasDia(h, extra)

    fun casos() = listOf(
        caso(1, ConfigPago(sueldoBase = 4800.0, asignacionFamiliar = true, sistemaPension = SistemaPension.AFP_INTEGRA, regimen = RegimenLaboral.GENERAL,
            apellidoPaterno = "Mendoza", apellidoMaterno = "Quispe", nombres = "Carlos Alberto", cargo = "Jefe de Operaciones", centroCosto = "OP-01",
            fechaIngreso = "2022-03-15", cuspp = "123456ABCDE1"), List(20) { d(8.0) } + d(8.0, 3.0)),
        caso(2, ConfigPago(sueldoBase = 2200.0, sistemaPension = SistemaPension.ONP, regimen = RegimenLaboral.PEQUENA_EMPRESA, fechaIngreso = "2026-11-11",
            apellidoPaterno = "Castro", apellidoMaterno = "Vargas", nombres = "Andrea Lucía"), List(15) { d(8.0) } + d(8.0, 2.5),
            PeriodoPlanilla(diasComputables = 20, faltas = 2, minutosTardanza = 45)),
        caso(3, ConfigPago(pagoPorHora = true, tarifaPorHora = 10.0, sistemaPension = SistemaPension.AFP_PRIMA, comisionMixta = true, asignacionFamiliar = true,
            apellidoPaterno = "Rojas", apellidoMaterno = "Huamán", nombres = "Valeria Sofía"), List(22) { d(8.0) } + d(8.0, 2.0),
            PeriodoPlanilla(faltas = 3)),
        caso(4, ConfigPago(pagoPorHora = true, tarifaPorHora = 6.0, sistemaPension = SistemaPension.AFP_HABITAT, regimen = RegimenLaboral.MICROEMPRESA),
            List(10) { d(3.0) }, doc = "001234567", tipo = TipoDocumento.CE),
        caso(5, ConfigPago(sueldoBase = 1230.0, sistemaPension = SistemaPension.ONP, regimen = RegimenLaboral.MICROEMPRESA, asignacionFamiliar = true),
            List(24) { d(8.0) }, PeriodoPlanilla(faltas = 1)),
        caso(6, ConfigPago(sueldoBase = 15000.0, sistemaPension = SistemaPension.AFP_PROFUTURO, asignacionFamiliar = true,
            apellidoPaterno = "Gómez", apellidoMaterno = "Silva", nombres = "Ricardo Enrique"), List(22) { d(8.0, 1.0) }),
        caso(7, ConfigPago(pagoPorHora = true, tarifaPorHora = 12.0, sistemaPension = SistemaPension.ONP), emptyList())
    )

    @Test fun libroCompleto_tieneTresHojasYFormulas() {
        val bytes = ByteArrayOutputStream().also { PlanillaExcel.escribir(it, empresa, casos(), mes, generadoEl = "2026-11-23") }.toByteArray()
        File("build/planilla_prueba.xlsx").writeBytes(bytes)
        val partes = mutableMapOf<String, String>()
        ZipInputStream(bytes.inputStream()).use { z ->
            generateSequence { z.nextEntry }.forEach { partes[it.name] = z.readBytes().toString(Charsets.UTF_8) }
        }
        assertTrue(partes.keys.containsAll(listOf("xl/workbook.xml", "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml")))
        assertTrue("Parametros_Legales" in partes["xl/workbook.xml"]!!)
        val planilla = partes["xl/worksheets/sheet2.xml"]!!
        assertTrue("Mendoza Quispe, Carlos Alberto" in planilla)
        assertTrue("<f>MAX(0,X6+Y6+AA6+AB6+AC6+AD6)</f>" in planilla)
        assertEquals(7, Regex("<c r=\"AM\\d+\"[^>]*><f>AE\\d+-AL\\d+</f>").findAll(planilla).count())
        assertTrue("001234567" in planilla) // C.E. conserva los ceros a la izquierda

        // Solo parámetros
        val soloP = ByteArrayOutputStream().also { PlanillaExcel.escribir(it, empresa, emptyList(), mes, incluirPlanilla = false) }.toByteArray()
        ZipInputStream(soloP.inputStream()).use { z ->
            val n = generateSequence { z.nextEntry }.count { it.name.startsWith("xl/worksheets/") }
            assertEquals(1, n)
        }
    }

    @Test fun nombreLegal_usaApellidosYNombres() {
        val u = Usuario(nombre = "carlos m", configPago = ConfigPago(apellidoPaterno = "Mendoza", apellidoMaterno = "Quispe", nombres = "Carlos Alberto"))
        assertEquals("Mendoza Quispe, Carlos Alberto", PlanillaExcel.nombreLegal(u))
        assertEquals("carlos m", PlanillaExcel.nombreLegal(Usuario(nombre = "carlos m")))
    }
}
