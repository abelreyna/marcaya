package com.juanreyna.registrotiempo

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * App Check en depuración: imprime en Logcat un token de depuración que debe registrarse
 * en Firebase Console → App Check → Administrar tokens de depuración.
 */
object AppCheckInstaller {
    fun instalar() {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
