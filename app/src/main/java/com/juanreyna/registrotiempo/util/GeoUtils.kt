package com.juanreyna.registrotiempo.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Validador de geocerca mediante la fórmula de Haversine (cálculo local, sin APIs de pago). */
object GeoUtils {
    private const val RADIO_TIERRA_METROS = 6_371_000.0

    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_METROS * asin(sqrt(a))
    }

    fun estaDentro(distanciaMetros: Double, radioMetros: Double): Boolean =
        distanciaMetros <= radioMetros
}
