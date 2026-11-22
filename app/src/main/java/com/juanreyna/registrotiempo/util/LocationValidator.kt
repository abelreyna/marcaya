package com.juanreyna.registrotiempo.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.juanreyna.registrotiempo.domain.model.Geocerca
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ResultadoUbicacion {
    data class Valida(
        val latitud: Double,
        val longitud: Double,
        val distanciaMetros: Double,
        val dentro: Boolean,
        val precisionMetros: Float
    ) : ResultadoUbicacion
    data object GpsFalso : ResultadoUbicacion
    data object SinSenal : ResultadoUbicacion
}

/** Lectura GPS con FusedLocationProviderClient + auditoría anti-Fake GPS + geocerca Haversine. */
@Singleton
class LocationValidator @Inject constructor(@ApplicationContext private val context: Context) {
    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    suspend fun validar(geocerca: Geocerca): ResultadoUbicacion {
        val cts = CancellationTokenSource()
        val loc: Location = runCatching {
            cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
        }.getOrNull() ?: return ResultadoUbicacion.SinSenal

        if (esUbicacionSimulada(loc)) return ResultadoUbicacion.GpsFalso

        val distancia = GeoUtils.distanciaMetros(loc.latitude, loc.longitude, geocerca.latitud, geocerca.longitud)
        return ResultadoUbicacion.Valida(
            latitud = loc.latitude,
            longitud = loc.longitude,
            distanciaMetros = distancia,
            dentro = GeoUtils.estaDentro(distancia, geocerca.radioMetros),
            precisionMetros = loc.accuracy
        )
    }

    @Suppress("DEPRECATION")
    private fun esUbicacionSimulada(loc: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) loc.isMock else loc.isFromMockProvider
}
