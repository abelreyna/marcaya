package com.juanreyna.registrotiempo

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Firebase App Check con Google Play Integrity API (solo builds firmadas desde Play Store). */
object AppCheckInstaller {
    fun instalar() {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
