package com.juanreyna.registrotiempo.util

import android.content.Context
import com.juanreyna.registrotiempo.data.messaging.AsistenciaMessagingService

/**
 * Lanza una notificación del sistema por cada aviso nuevo (incidencia, alerta o justificación),
 * una sola vez: recuerda las claves ya notificadas en preferencias locales.
 * En la primera ejecución registra lo existente sin notificar, para no inundar al usuario con el historial.
 */
object AvisosIncidencias {

    data class Aviso(val clave: String, val titulo: String, val cuerpo: String)

    private const val PREFS = "avisos_incidencias"
    private const val MAX = 400

    fun notificarNuevos(context: Context, usuarioId: String, avisos: List<Aviso>) {
        if (avisos.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = "vistos_$usuarioId"
        val primeraVez = !prefs.contains(k)
        val vistos = prefs.getStringSet(k, emptySet()).orEmpty().toMutableSet()
        val nuevos = avisos.filter { it.clave !in vistos }
        if (nuevos.isEmpty()) return
        if (!primeraVez) {
            if (nuevos.size > 3) {
                AsistenciaMessagingService.mostrar(context, "${nuevos.size} nuevas incidencias", nuevos.take(3).joinToString(" · ") { it.titulo })
            } else nuevos.forEach { AsistenciaMessagingService.mostrar(context, it.titulo, it.cuerpo) }
        }
        vistos += nuevos.map { it.clave }
        prefs.edit().putStringSet(k, if (vistos.size > MAX) vistos.toList().takeLast(MAX).toSet() else vistos).apply()
    }
}
