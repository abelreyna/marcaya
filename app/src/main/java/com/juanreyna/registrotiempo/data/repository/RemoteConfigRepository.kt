package com.juanreyna.registrotiempo.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.juanreyna.registrotiempo.BuildConfig
import com.juanreyna.registrotiempo.domain.vip.VipConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Parámetros del muro VIP controlados desde Firebase Remote Config (sin publicar en Play Store). */
@Singleton
class RemoteConfigRepository @Inject constructor() {
    private val rc: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 12 * 60 * 60)
                    .build()
            )
            setDefaultsAsync(
                mapOf(
                    "vip_gate_enabled" to false,
                    "vip_free_trial_days" to 365L,
                    "vip_warning_days_before" to 15L,
                    "vip_price_monthly_text" to "S/ 29.90 / mes",
                    "vip_price_annual_text" to "S/ 290.00 / año",
                    "vip_promo_message" to "Disfruta de todas las funciones gratis...",
                    "vip_hard_block" to false
                )
            )
        }
    }

    suspend fun cargar(): VipConfig {
        runCatching { rc.fetchAndActivate().await() }
        return VipConfig(
            isGateEnabled = rc.getBoolean("vip_gate_enabled"),
            freeTrialDays = rc.getLong("vip_free_trial_days"),
            warningDaysBefore = rc.getLong("vip_warning_days_before"),
            monthlyPrice = rc.getString("vip_price_monthly_text"),
            annualPrice = rc.getString("vip_price_annual_text"),
            promoMessage = rc.getString("vip_promo_message"),
            isHardBlock = rc.getBoolean("vip_hard_block")
        )
    }
}
