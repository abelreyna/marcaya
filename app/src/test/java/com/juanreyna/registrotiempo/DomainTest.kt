package com.juanreyna.registrotiempo

import com.juanreyna.registrotiempo.domain.calc.DetectorIncidencias
import com.juanreyna.registrotiempo.domain.calc.JornadaCalculator
import com.juanreyna.registrotiempo.domain.calc.ParametrosLegales
import com.juanreyna.registrotiempo.domain.calc.PeriodoPlanilla
import com.juanreyna.registrotiempo.domain.calc.HorasDia
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

    @Test fun modoAvanzado_sueldoMensualCompletoMasExtras() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 2400.0, sistemaPension = SistemaPension.ONP, regimen = RegimenLaboral.GENERAL)
        // El sueldo mensual se paga completo (no por horas); un día de 11 h suma 2 h al 25 % y 1 h al 35 % (valor hora = 2400 / 240 = 10)
        val b = PlanillaPeruCalculator.calcular(cfg, listOf(11.0))
        assertEquals(2400.0 + 25.0 + 13.5, b.totalIngresos, 0.001)
        assertEquals(2438.5 * 0.13, b.ingresos.let { b.descuentos.first { it.concepto.startsWith("ONP") }.monto }, 0.01)
        assertEquals(30, b.diasLaborados)
    }

    // ---------- Auditoría Modo Avanzado Perú (noviembre 2026: RMV S/ 1,230, UIT S/ 5,500) ----------
    private val nov26 = ParametrosLegales.delMes("2026-11")
    private fun monto(b: com.juanreyna.registrotiempo.domain.calc.Boleta, prefijo: String) =
        (b.ingresos + b.descuentos + b.aportesEmpleador + b.provisiones).first { it.concepto.startsWith(prefijo) }.monto

    // ---------- Modo Avanzado: remuneración por hora ----------
    @Test fun avanzadoPorHora_pagaCadaHoraConBeneficios() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, pagoPorHora = true, tarifaPorHora = 10.0, sistemaPension = SistemaPension.ONP)
        // 22 días de 8 h + 1 día de 10 h (2 h extra al 25 %)
        val b = PlanillaPeruCalculator.calcular(cfg, List(22) { 8.0 } + 10.0, nov26)
        assertEquals(1840.0, monto(b, "Remuneración por horas"), 0.001)
        assertEquals(25.0, monto(b, "Horas extras 25"), 0.001)
        assertEquals(1865.0, b.totalIngresos, 0.001)
        assertEquals(242.45, monto(b, "ONP"), 0.001)
        assertEquals(167.85, monto(b, "EsSalud"), 0.001)
        assertEquals(306.67, monto(b, "Provisión gratificación"), 0.001)
        assertEquals(27.60, monto(b, "Provisión bonificación"), 0.001)
        assertEquals(178.89, monto(b, "Provisión CTS"), 0.001)
        assertEquals(153.33, monto(b, "Provisión vacaciones"), 0.001)
        assertEquals(23, b.diasLaborados)
        assertEquals(10.0, cfg.valorHora, 0.0)
        assertEquals(10.0, ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 2400.0).valorHora, 0.0)
    }

    @Test fun avanzadoPorHora_tiempoParcialYSinDescuentoPorFaltas() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, pagoPorHora = true, tarifaPorHora = 6.0, sistemaPension = SistemaPension.ONP)
        val b = PlanillaPeruCalculator.calcular(cfg, List(10) { 3.0 }, nov26, PeriodoPlanilla(faltas = 3, minutosTardanza = 40))
        assertEquals(180.0, b.totalIngresos, 0.001)                 // sin línea de tardanzas ni descuento por faltas
        assertEquals(0, b.faltas)
        assertEquals(110.70, monto(b, "EsSalud"), 0.001)             // base mínima: una RMV
        assertEquals(30.0, monto(b, "Provisión gratificación"), 0.001)
        assertTrue(b.provisiones.none { it.concepto.startsWith("Provisión CTS") || it.concepto.startsWith("Provisión vacaciones") })
        assertTrue(b.advertencias.any { "tiempo parcial" in it })
        val bajo = PlanillaPeruCalculator.calcular(cfg.copy(tarifaPorHora = 5.0), listOf(8.0), nov26)
        assertTrue(bajo.advertencias.any { "RMV por hora" in it })
        // Un mes sin horas no genera pago ni EsSalud
        assertEquals(0.0, PlanillaPeruCalculator.calcular(cfg, emptyList(), nov26).costoEmpleador, 0.001)
    }

    @Test fun avanzadoPorHora_costoDiario() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, pagoPorHora = true, tarifaPorHora = 10.0)
        // 80 + EsSalud 7.20 + grati 13.33 + bonif 1.20 + CTS 7.78 + vacaciones 6.67
        assertEquals(116.18, PlanillaPeruCalculator.costoDiario(cfg, HorasDia(8.0, 0.0), nov26), 0.001)
    }

    @Test fun parametros_vigentesPorMes() {
        assertEquals(1230.0, nov26.rmv, 0.0)
        assertEquals(5500.0, nov26.uit, 0.0)
        assertEquals(1130.0, ParametrosLegales.delMes("2026-09").rmv, 0.0)   // antes del DS 015-2026-TR
        assertEquals(5500.0, ParametrosLegales.delMes("2026-09").uit, 0.0)
        assertEquals(5350.0, ParametrosLegales.delMes("2025-12").uit, 0.0)
        assertEquals(0.0137, nov26.primaSeguroAfp, 0.0)
    }

    @Test fun avanzado_regimenGeneral_afpConAsignacionYExtras() {
        // Sueldo 3000 + asignación familiar 123 (10 % de 1230); 3 h extras en un día; AFP Integra comisión flujo
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 3000.0, asignacionFamiliar = true,
            sistemaPension = SistemaPension.AFP_INTEGRA, regimen = RegimenLaboral.GENERAL)
        val b = PlanillaPeruCalculator.calcularDias(cfg, listOf(HorasDia(8.0, 3.0)), nov26)
        // Valor hora extra = (3000 + 123) / 240 = 13.0125 → 2 h × 1.25 = 32.53 ; 1 h × 1.35 = 17.57
        assertEquals(3000.0, monto(b, "Remuneración básica"), 0.001)
        assertEquals(123.0, monto(b, "Asignación familiar"), 0.001)
        assertEquals(32.53, monto(b, "Horas extras 25"), 0.001)
        assertEquals(17.57, monto(b, "Horas extras 35"), 0.001)
        assertEquals(3173.10, b.totalIngresos, 0.001)
        // AFP sobre 3173.10: aporte 10 % = 317.31 ; prima 1.37 % = 43.47 ; comisión 1.55 % = 49.18
        assertEquals(317.31, monto(b, "AFP Integra — Aporte"), 0.001)
        assertEquals(43.47, monto(b, "AFP Integra — Prima"), 0.001)
        assertEquals(49.18, monto(b, "AFP Integra — Comisión"), 0.001)
        // 5.ª: 3123 × 12 + 3123 × 2 × 1.09 + 50.10 = 44 334.24 − 7 UIT (38 500) = 5 834.24 × 8 % = 466.74 / 12 = 38.895 → 38.90
        assertEquals(38.90, monto(b, "Renta de 5.ª"), 0.001)
        assertEquals(2724.24, b.netoAPagar, 0.001)
        // Empleador: EsSalud 9 % = 285.58 ; gratificación 3123/6 = 520.50 ; bonificación 9 % = 46.85 ;
        // CTS (3123 + 520.50) / 12 = 303.63 ; vacaciones 3123 / 12 = 260.25
        assertEquals(285.58, monto(b, "EsSalud"), 0.001)
        assertEquals(520.50, monto(b, "Provisión gratificación"), 0.001)
        assertEquals(46.85, monto(b, "Provisión bonificación"), 0.001)
        assertEquals(303.63, monto(b, "Provisión CTS"), 0.001)
        assertEquals(260.25, monto(b, "Provisión vacaciones"), 0.001)
        assertEquals(3173.10 + 285.58 + 520.50 + 46.85 + 303.63 + 260.25, b.costoEmpleador, 0.001)
        assertTrue(b.advertencias.none { it.contains("RMV") })
    }

    @Test fun avanzado_faltasYTardanzasSeDescuentan() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 1500.0, sistemaPension = SistemaPension.ONP)
        val b = PlanillaPeruCalculator.calcularDias(cfg, emptyList(), nov26, PeriodoPlanilla(faltas = 2, minutosTardanza = 90))
        // 1500 / 30 × 28 = 1400 ; tardanza 1.5 h × (1500 / 240) = 9.38
        assertEquals(1400.0, monto(b, "Remuneración básica"), 0.001)
        assertEquals(-9.38, monto(b, "(−) Tardanzas"), 0.001)
        assertEquals(1390.62, b.totalIngresos, 0.001)
        assertEquals(180.78, monto(b, "ONP"), 0.001)              // 13 % de 1390.62
        assertTrue(b.descuentos.none { it.concepto.startsWith("Renta") }) // 21 270 al año < 7 UIT
        assertEquals(125.16, monto(b, "EsSalud"), 0.001)          // 9 % de 1390.62 (mayor que la RMV)
        assertEquals(136.11, monto(b, "Provisión CTS"), 0.001)    // (1500 + 250) / 12 × 28/30
        assertEquals(116.67, monto(b, "Provisión vacaciones"), 0.001) // 1500 / 12 × 28/30
        assertEquals(28, b.diasLaborados)
        assertEquals(2, b.faltas)
    }

    @Test fun avanzado_pequenaEmpresa_ingresoAMitadDeMes() {
        // Ingresa el 16/11: 15 días ; AFP Prima con comisión mixta ; asignación familiar por acuerdo
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 1230.0, asignacionFamiliar = true,
            sistemaPension = SistemaPension.AFP_PRIMA, comisionMixta = true, regimen = RegimenLaboral.PEQUENA_EMPRESA)
        val b = PlanillaPeruCalculator.calcularDias(cfg, emptyList(), nov26, PeriodoPlanilla(diasComputables = 15))
        assertEquals(615.0, monto(b, "Remuneración básica"), 0.001)
        assertEquals(738.0, b.totalIngresos, 0.001)
        assertEquals(73.80, monto(b, "AFP Prima — Aporte"), 0.001)
        assertEquals(10.11, monto(b, "AFP Prima — Prima"), 0.001)
        assertTrue(b.descuentos.none { it.concepto.contains("Comisión") })   // mixta: sin comisión sobre flujo
        assertEquals(66.42, monto(b, "EsSalud"), 0.001)
        assertTrue(b.provisiones.none { it.concepto.contains("gratificación") }) // mes incompleto: no suma gratificación
        // CTS pequeña empresa = ½ × (1353 + 1353 × ½ / 6) / 12 × 15/30 = 30.54 ; vacaciones 15 días = 1353 / 24 × ½ = 28.19
        assertEquals(30.54, monto(b, "Provisión CTS"), 0.001)
        assertEquals(28.19, monto(b, "Provisión vacaciones"), 0.001)
        assertTrue(b.advertencias.any { it.contains("REMYPE") })
    }

    @Test fun avanzado_microempresa_sinCtsNiGratificacion() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 1230.0, regimen = RegimenLaboral.MICROEMPRESA)
        val b = PlanillaPeruCalculator.calcularDias(cfg, emptyList(), nov26)
        assertEquals(159.90, monto(b, "ONP"), 0.001)
        assertTrue(b.provisiones.none { it.concepto.contains("CTS") || it.concepto.contains("gratificación") })
        assertEquals(51.25, monto(b, "Provisión vacaciones"), 0.001)   // 15 días: 1230 / 24
        assertTrue(b.aportesEmpleador.none { it.concepto.startsWith("EsSalud") })
    }

    @Test fun avanzado_quintaCategoriaYTopeDePrima() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 10000.0, sistemaPension = SistemaPension.AFP_HABITAT)
        val b = PlanillaPeruCalculator.calcularDias(cfg, emptyList(), nov26)
        // 120 000 + 20 000 × 1.09 = 141 800 − 38 500 = 103 300 → 27 500 × 8 % + 75 800 × 14 % = 12 812 / 12 = 1 067.67
        assertEquals(1067.67, monto(b, "Renta de 5.ª"), 0.001)
        assertEquals(137.0, monto(b, "AFP Hábitat — Prima"), 0.001)
        assertEquals(147.0, monto(b, "AFP Hábitat — Comisión"), 0.001)
        // Sueldo mayor que la remuneración máxima asegurable: la prima se calcula sobre el tope (12 732.70)
        val alto = PlanillaPeruCalculator.calcularDias(cfg.copy(sueldoBase = 15000.0), emptyList(), nov26)
        assertEquals(174.44, monto(alto, "AFP Hábitat — Prima"), 0.001)
        assertEquals(1500.0, monto(alto, "AFP Hábitat — Aporte"), 0.001)
    }

    @Test fun avanzado_sueldoMenorQueRmvAdvierte() {
        val b = PlanillaPeruCalculator.calcularDias(ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 1130.0), emptyList(), nov26)
        assertTrue(b.advertencias.any { it.contains("RMV") })
        // La base mínima de EsSalud es la RMV vigente
        assertEquals(110.70, monto(b, "EsSalud"), 0.001)
    }

    @Test fun avanzado_costoDiarioDelPanel() {
        val cfg = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 3000.0, asignacionFamiliar = true, sistemaPension = SistemaPension.AFP_INTEGRA)
        val mes = PlanillaPeruCalculator.calcularDias(cfg, emptyList(), nov26)
        assertEquals(mes.costoEmpleador / 30, PlanillaPeruCalculator.costoDiario(cfg, HorasDia(8.0, 0.0), nov26), 0.01)
        assertEquals(0.0, PlanillaPeruCalculator.costoDiario(cfg, HorasDia(0.0, 0.0), nov26), 0.0)
        // Antes se sumaban la asignación, el EsSalud mínimo y las provisiones de todo un mes en un solo día
        assertTrue(PlanillaPeruCalculator.costoDiario(cfg, HorasDia(8.0, 0.0), nov26) < 200.0)
    }

    @Test fun avanzado_periodoDesdeFechaDeIngreso() {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Lima")).apply {
            set(2026, 10, 16, 9, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val u = Usuario(uid = "x", creadoMillis = cal.timeInMillis,
            configPago = ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 1500.0, fechaIngreso = "2026-11-16"))
        val ahora = cal.timeInMillis + 6L * 24 * 3600 * 1000 // domingo 22/11
        val p = ResumenMensual.periodoDe(u, emptyList(), emptyList(), "2026-11", ahora)
        assertEquals(15, p.diasComputables)
        assertEquals(6, p.faltas) // lunes 16 a sábado 21 sin marcar; no cuenta antes del ingreso ni el domingo
        // Una falta justificada (aprobada) ya no se descuenta
        val j = Justificacion(colaboradorId = "x", fecha = "2026-11-16", incidencia = TipoIncidencia.FALTA,
            motivo = MotivoJustificacion.CITA_MEDICA, estado = EstadoJustificacion.APROBADA)
        assertEquals(5, ResumenMensual.periodoDe(u, emptyList(), listOf(j), "2026-11", ahora).faltas)
    }

    @Test fun horario_distintoPorDia() {
        // Lunes 08:00–17:00 (general); martes solo en la tarde 14:00–19:00 sin descanso; domingo en la mañana
        val h = Horario(
            diasLaborables = listOf(1, 2, 7),
            turnos = mapOf(
                2 to com.juanreyna.registrotiempo.domain.model.Turno("14:00", "19:00", "19:00", "19:00"),
                7 to com.juanreyna.registrotiempo.domain.model.Turno("08:00", "12:00", "12:00", "12:00")
            )
        )
        assertTrue(h.porDia)
        assertEquals("08:00", h.deFecha("2026-11-23").entrada)          // lunes: horario general
        assertEquals("14:00", h.deFecha("2026-11-24").entrada)          // martes: tarde
        assertEquals(300, h.deFecha("2026-11-24").minutosJornadaProgramada)
        assertEquals(240, h.deFecha("2026-11-22").minutosJornadaProgramada) // domingo: mañana

        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Lima")).apply {
            set(2026, 10, 24, 0, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        fun a(hh: Int, m: Int) = cal.timeInMillis + (hh * 60 + m) * 60_000L
        // Martes: entra 14:20 (20 min tarde según el turno de la tarde) y sale 20:00 (1 h extra)
        val dia = ResumenMensual.diaDe("2026-11-24", listOf(
            Marcaje(tipo = TipoMarcaje.INGRESO, timestampMillis = a(14, 20)),
            Marcaje(tipo = TipoMarcaje.SALIDA, timestampMillis = a(20, 0))
        ), a(23, 0), h)
        assertEquals(20L, dia.minutosTarde)
        assertEquals(60L, dia.minutosExtra)
        // Sin descanso programado ese día: salir a descanso no genera incidencia
        assertEquals(null, com.juanreyna.registrotiempo.domain.calc.ReglasMarcaje.evaluar(h.deFecha("2026-11-24"), TipoMarcaje.INICIO_DESCANSO, a(16, 0)))
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

        // Modo Simple: 445 min ordinarios + 2.5 h extras a la misma tarifa, sin recargo
        val b = PlanillaPeruCalculator.calcularDias(ConfigPago(modo = ModoPago.SIMPLE, tarifaPorHora = 10.0), listOf(dia.horas))
        assertEquals(74.17 + 25.0, b.totalIngresos, 0.01)
        // Modo Avanzado Perú: mismas horas con recargos de ley (+25 % las 2 primeras, +35 % desde la tercera)
        val bAv = PlanillaPeruCalculator.calcularDias(ConfigPago(modo = ModoPago.AVANZADO, sueldoBase = 2400.0), listOf(dia.horas))
        assertEquals(2 * 10.0 * 1.25, bAv.ingresos.first { it.concepto.startsWith("Horas extras 25") }.monto, 0.01)
        assertEquals(0.5 * 10.0 * 1.35, bAv.ingresos.first { it.concepto.startsWith("Horas extras 35") }.monto, 0.01)

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
