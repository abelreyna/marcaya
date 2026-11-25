package com.juanreyna.registrotiempo

import android.app.Application
import com.juanreyna.registrotiempo.data.messaging.AsistenciaMessagingService
import com.juanreyna.registrotiempo.data.remote.FirebaseState
import dagger.hilt.android.HiltAndroidApp

/**
 * Marcaya v1.0.0
 * Autor y desarrollador: Juan Abel Reyna Gonzales — Ingeniero Industrial
 */
@HiltAndroidApp
class RegistroTiempoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AsistenciaMessagingService.crearCanal(this)
        if (FirebaseState.estaConfigurado(this)) {
            // Play Integrity en release; proveedor de depuración en debug (ver src/debug y src/release)
            AppCheckInstaller.instalar()
        }
    }
}
