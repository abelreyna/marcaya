package com.juanreyna.registrotiempo

import com.juanreyna.registrotiempo.domain.calc.JornadaCalculator
import com.juanreyna.registrotiempo.domain.calc.ParametrosLegales
import com.juanreyna.registrotiempo.domain.calc.PlanillaPeruCalculator
import com.juanreyna.registrotiempo.domain.model.ConfigPago
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
}
