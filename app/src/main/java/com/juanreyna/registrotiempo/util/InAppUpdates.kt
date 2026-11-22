package com.juanreyna.registrotiempo.util

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play In-App Updates: IMMEDIATE para parches prioritarios (prioridad ≥ 4 en Play Console),
 * FLEXIBLE para mejoras visuales.
 */
class InAppUpdates(private val activity: ComponentActivity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    fun verificar() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
            val tipo = if (info.updatePriority() >= 4 && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
                AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
            if (!info.isUpdateTypeAllowed(tipo)) return@addOnSuccessListener
            if (tipo == AppUpdateType.FLEXIBLE) {
                manager.registerListener { estado ->
                    if (estado.installStatus() == InstallStatus.DOWNLOADED) manager.completeUpdate()
                }
            }
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(tipo).build())
        }
    }

    /** Reanuda una actualización inmediata interrumpida. */
    fun reanudar() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
            }
        }
    }
}
