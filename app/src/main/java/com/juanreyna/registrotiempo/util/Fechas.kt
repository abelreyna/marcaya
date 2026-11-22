package com.juanreyna.registrotiempo.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Utilidades de fecha en zona horaria de Perú (America/Lima). La hora oficial siempre viene del servidor. */
object Fechas {
    val zonaLima: TimeZone = TimeZone.getTimeZone("America/Lima")
    private val localePe = Locale("es", "PE")

    private fun fmt(patron: String) = SimpleDateFormat(patron, localePe).apply { timeZone = zonaLima }

    fun hoy(): String = fmt("yyyy-MM-dd").format(Date())
    fun mesActual(): String = fmt("yyyy-MM").format(Date())
    fun hora(millis: Long?): String = millis?.let { fmt("HH:mm:ss").format(Date(it)) } ?: "--:--"
    fun fechaLarga(millis: Long): String = fmt("EEEE d 'de' MMMM, yyyy").format(Date(millis))
    fun etiquetaMes(mes: String): String = runCatching {
        fmt("MMMM yyyy").format(fmt("yyyy-MM").parse(mes)!!).replaceFirstChar { it.uppercase() }
    }.getOrDefault(mes)

    fun mesAnterior(mes: String): String {
        val c = Calendar.getInstance(zonaLima).apply { time = fmt("yyyy-MM").parse(mes)!!; add(Calendar.MONTH, -1) }
        return fmt("yyyy-MM").format(c.time)
    }

    /** "2026-11-17" → ("Mar", "17", "Nov") para la insignia del historial. */
    fun partesFecha(fecha: String): Triple<String, String, String> = runCatching {
        val d = fmt("yyyy-MM-dd").parse(fecha)!!
        Triple(
            fmt("EEE").format(d).take(3).replaceFirstChar { it.uppercase() },
            fmt("d").format(d),
            fmt("MMM").format(d).take(3).replaceFirstChar { it.uppercase() }
        )
    }.getOrDefault(Triple("", fecha, ""))

    fun horaCorta(millis: Long?): String = millis?.let { fmt("hh:mm a").format(Date(it)).uppercase() } ?: "--:--"

    fun mesSiguiente(mes: String): String {
        val c = Calendar.getInstance(zonaLima).apply { time = fmt("yyyy-MM").parse(mes)!!; add(Calendar.MONTH, 1) }
        return fmt("yyyy-MM").format(c.time)
    }

    /** Tardanza calculada con la hora oficial del servidor (tolerancia de 5 minutos). */
    fun esTardanza(ingresoMillis: Long?, horaEsperada: String, toleranciaMin: Int = 5): Boolean =
        ingresoMillis != null && minutosDelDia(ingresoMillis) > parseHora(horaEsperada) + toleranciaMin

    /** Minutos desde medianoche (hora Lima) del instante indicado. */
    fun minutosDelDia(millis: Long): Int {
        val c = Calendar.getInstance(zonaLima).apply { timeInMillis = millis }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    fun parseHora(hhmm: String): Int = runCatching {
        val (h, m) = hhmm.split(":").map { it.trim().toInt() }
        h * 60 + m
    }.getOrDefault(8 * 60)

    fun duracion(minutos: Long): String = "%dh %02dm".format(minutos / 60, minutos % 60)
}
