package com.juanreyna.registrotiempo.data.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.juanreyna.registrotiempo.R

/** Notificaciones push (FCM): alertas de geocerca, tardanzas, aprobaciones y recordatorios de descanso. */
class AsistenciaMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: message.data["titulo"] ?: return
        val cuerpo = message.notification?.body ?: message.data["cuerpo"].orEmpty()
        mostrar(this, titulo, cuerpo)
    }

    companion object {
        const val CANAL = "alertas_asistencia"

        fun crearCanal(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    CANAL, context.getString(R.string.canal_notificaciones), NotificationManager.IMPORTANCE_HIGH
                )
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
            }
        }

        fun mostrar(context: Context, titulo: String, cuerpo: String) {
            val n = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), n) }
        }
    }
}
