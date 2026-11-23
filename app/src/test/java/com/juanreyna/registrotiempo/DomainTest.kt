package com.juanreyna.registrotiempo

import com.juanreyna.registrotiempo.domain.calc.DetectorIncidencias
import com.juanreyna.registrotiempo.domain.calc.JornadaCalculator
import com.juanreyna.registrotiempo.domain.calc.ParametrosLegales
import com.juanreyna.registrotiempo.domain.calc.PlanillaPeruCalculator
import com.juanreyna.registrotiempo.domain.calc.ResumenMensual
import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.EstadoIncidencia
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.MotivoJustificacion
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.SistemaPension
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.domain.vip.AccessStatus
import com.juanreyna.registrotiempo.domain.vip.VipAccessManager
import com.juanreyna.registrotiempo.domain.vip.VipConfig
import com.juanreyna.registrotiempo.util.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val h = 3_600_000L

    @Test fun haversine_plazaDeArmasLima_aproxCorrecta() {
        // Plaza de Armas de Lima → Palacio de Justicia ≈ 1.6 km
        val d = GeoUtils.distanciaMetros(-12.0464, -77.0428, -12.0600, -77.0365)
        assertTrue(d in 1500.0..1800.0)
    }

    @Test fun jornada_descuentaDescanso() {
        val base = 1_000_000_000L
        val marcajes = listOf(
            Marcaje(tipo = TipoMarcaje.INGRESO, timestampMillis = base),
            Marcaje(tipo = TipoMarcaje.INICIO_DESCANSO, timestampMillis = base + 4 * h),
            Marcaje(tipo = TipoMarcaje.FIN_DESCANSO, timestampMillis = base + 5 * h),
            Marcaje(tipo = TipoMarcaje.SALIDA, timestampMillis = base + 9 * h)
        )
        val r = JornadaCalculator.resumir(marcajes, base + 20 * h)
        assertEquals(8 * 60L, r.minutosEfectivos)
        assertEquals(60L, r.minutosDescanso)
    }

    @Test fun modoSimple_horasPorTarifa() {
        val b = PlanillaPeruCalculator.calcular(ConfigPago(modo = ModoPago.SIMPLE, tarifaPorHora = 12.0), listOf(8.0, 7.5))
        assertEquals(186.0, b.netoAPagar, 0.001)
    }

    @Test fun modoAvanzado_horasExtrasYOnp() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 2400.0, sistemaPension = SistemaPension.ONP, regimen = RegimenLaboral.GENERAL)
        // Un día de 11 h: 8 ordinarias + 2 al 25 % + 1 al 35 %; valor hora = 10
        val b = PlanillaPeruCalculator.calcular(cfg, listOf(11.0))
        assertEquals(80.0 + 25.0 + 13.5, b.totalIngresos, 0.001)
        assertEquals((118.5 * 0.13), b.totalDescuentos, 0.01)
    }

    @Test fun horario_tardanzaExcesoDescansoYExtras() {
        // Día en hora de Lima: 08:00–17:00, descanso 13:00–14:00, tolerancia 5 min
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Lima")).apply {
            set(2026, 10, 20, 0, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val medianoche = cal.timeInMillis
        fun a(h: Int, m: Int) = medianoche + (h * 60 + m) * 60_000L
        val marcajes = listOf(
            Marcaje(tipo = TipoMarcaje.INGRESO, timestampMillis = a(8, 20)),
            Marcaje(tipo = TipoMarcaje.INICIO_DESCANSO, timestampMillis = a(13, 0)),
            Marcaje(tipo = TipoMarcaje.FIN_DESCANSO, timestampMillis = a(14, 15)),
            Marcaje(tipo = TipoMarcaje.SALIDA, timestampMillis = a(19, 30))
        )
        val dia = ResumenMensual.diaDe("2026-11-20", marcajes, a(23, 0), Horario())
        assertTrue(dia.tardanza)
        assertEquals(20L, dia.minutosTarde)
        assertEquals(15L, dia.minutosExcesoDescanso)   // 75 min de descanso − 60 programados
        assertEquals(150L, dia.minutosExtra)           // 17:00 → 19:30
        assertEquals(595L, dia.minutosEfectivos)       // el exceso de descanso no cuenta como trabajado

        // Modo Simple con recargo configurable 25 % / 35 %: 445 min ordinarios + 2 h al 25 % + 0.5 h al 35 %
        val b = PlanillaPeruCalculator.calcularDias(ConfigPago(modo = ModoPago.SIMPLE, tarifaPorHora = 10.0), listOf(dia.horas))
        assertEquals(74.17 + 25.0 + 6.75, b.totalIngresos, 0.01)

        // Dentro de la tolerancia no hay tardanza ni extras
        val puntual = listOf(
            Marcaje(tipo = TipoMarcaje.INGRESO, timestampMillis = a(8, 4)),
            Marcaje(tipo = TipoMarcaje.SALIDA, timestampMillis = a(17, 3))
        )
        val d2 = ResumenMensual.diaDe("2026-11-20", puntual, a(23, 0), Horario())
        assertEquals(false, d2.tardanza)
        assertEquals(0L, d2.minutosExtra)
    }

    @Test fun quinta_tramos() {
        val p = ParametrosLegales(uit = 5000.0)
        // Renta neta = 60 000 − 35 000 = 25 000 = 5 UIT → 8 %
        assertEquals(2000.0, PlanillaPeruCalculator.impuestoQuintaAnual(60_000.0, p), 0.01)
        // Renta neta 35 000: 25 000×8 % + 10 000×14 %
        assertEquals(3400.0, PlanillaPeruCalculator.impuestoQuintaAnual(70_000.0, p), 0.01)
    }

    @Test fun vip_estados() {
        val dia = 86_400_000L
        val cfg = VipConfig(isGateEnabled = true, freeTrialDays = 365, warningDaysBefore = 15, isHardBlock = true)
        assertEquals(AccessStatus.GRANTED, VipAccessManager.evaluateCompanyAccess("VIP", 0, 400 * dia, cfg))
        assertEquals(AccessStatus.TRIAL_ACTIVE, VipAccessManager.evaluateCompanyAccess("FREE", 0, 10 * dia, cfg))
        assertEquals(AccessStatus.EXPIRING_SOON, VipAccessManager.evaluateCompanyAccess("FREE", 0, 355 * dia, cfg))
        assertEquals(AccessStatus.BLOCKED_NEED_VIP, VipAccessManager.evaluateCompanyAccess("FREE", 0, 366 * dia, cfg))
    }

    @Test fun incidencias_faltasTardanzaYJustificaciones() {
        val lima = java.util.TimeZone.getTimeZone("America/Lima")
        fun t(dia: Int, h: Int, m: Int) = java.util.Calendar.getInstance(lima).apply {
            set(2026, 10, dia, h, m, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        // Registrado el martes 17/11; horario 08:00–17:00 lunes a sábado; "ahora" = viernes 20/11 a las 23:00
        val u = Usuario(uid = "c1", nombre = "Ana", creadoMillis = t(17, 0, 0))
        fun mk(dia: Int, tipo: TipoMarcaje, h: Int, m: Int) =
            Marcaje(colaboradorId = "c1", fecha = "2026-11-%02d".format(dia), mes = "2026-11", tipo = tipo, timestampMillis = t(dia, h, m), dentroGeocerca = true)
        val ms = listOf(
            mk(18, TipoMarcaje.INGRESO, 8, 20), mk(18, TipoMarcaje.SALIDA, 15, 0),   // tardanza y salida anticipada
            mk(20, TipoMarcaje.INGRESO, 8, 0), mk(20, TipoMarcaje.INICIO_DESCANSO, 13, 0),
            mk(20, TipoMarcaje.FIN_DESCANSO, 14, 0), mk(20, TipoMarcaje.SALIDA, 17, 0) // día puntual
        )
        val js = listOf(
            // Justificación antigua (sin tipo): cubre todas las incidencias del 18
            Justificacion(id = "j1", colaboradorId = "c1", fecha = "2026-11-18", motivo = MotivoJustificacion.CITA_MEDICA, estado = EstadoJustificacion.APROBADA),
            Justificacion(id = "j2", colaboradorId = "c1", fecha = "2026-11-19", motivo = MotivoJustificacion.OTRO, estado = EstadoJustificacion.RECHAZADA, incidencia = TipoIncidencia.FALTA)
        )
        val inc = DetectorIncidencias.detectar(u, ms, js, "2026-11", t(20, 23, 0))
        fun de(fecha: String, tipo: TipoIncidencia) = inc.single { it.fecha == fecha && it.tipo == tipo }

        assertEquals(EstadoIncidencia.POR_JUSTIFICAR, de("2026-11-17", TipoIncidencia.FALTA).estado)
        assertEquals(EstadoIncidencia.JUSTIFICADA, de("2026-11-18", TipoIncidencia.TARDANZA).estado)
        assertEquals(20L, de("2026-11-18", TipoIncidencia.TARDANZA).minutos)
        assertEquals(120L, de("2026-11-18", TipoIncidencia.SALIDA_ANTICIPADA).minutos)
        assertEquals("Falta no autorizada", de("2026-11-19", TipoIncidencia.FALTA).titulo)
        assertTrue(inc.none { it.fecha == "2026-11-20" || it.fecha == "2026-11-16" }) // puntual / antes del registro
        assertEquals(4, inc.size)
    }

    @Test
    fun documentoYCelular_digitosExactos() {
        val dni = com.juanreyna.registrotiempo.domain.model.TipoDocumento.DNI
        val ce = com.juanreyna.registrotiempo.domain.model.TipoDocumento.CE
        val cel = com.juanreyna.registrotiempo.domain.model.Celular
        assertEquals("12345678", dni.limpiar("12a345678999"))
        assertTrue(dni.esValido("12345678")); assertFalse(dni.esValido("1234567")); assertFalse(dni.esValido("123456789"))
        assertTrue(ce.esValido("001234567")); assertFalse(ce.esValido("12345678"))
        assertEquals("987654321", cel.limpiar("+51 987 654 321"))
        assertTrue(cel.esValido("987654321")); assertFalse(cel.esValido("187654321")); assertFalse(cel.esValido("98765432"))
    }
}
