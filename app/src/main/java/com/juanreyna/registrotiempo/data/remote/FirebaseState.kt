package com.juanreyna.registrotiempo.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp

/** Indica si el proyecto incluye app/google-services.json (Firebase inicializado). */
object FirebaseState {
    fun estaConfigurado(context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null

    /** ID de cliente web OAuth generado por el plugin google-services (default_web_client_id). */
    fun webClientId(context: Context): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id) else null
    }
}
