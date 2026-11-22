package com.juanreyna.registrotiempo.domain.vip

data class VipConfig(
    val isGateEnabled: Boolean = false,
    val freeTrialDays: Long = 365,
    val warningDaysBefore: Long = 15,
    val monthlyPrice: String = "S/ 29.90 / mes",
    val annualPrice: String = "S/ 290.00 / año",
    val promoMessage: String = "Disfruta de todas las funciones gratis...",
    val isHardBlock: Boolean = false
)

enum class AccessStatus {
    GRANTED,          // Acceso libre (Plan VIP activo o muro desactivado)
    TRIAL_ACTIVE,     // Dentro del periodo gratuito (ej. primer año)
    EXPIRING_SOON,    // Faltan <= N días: mostrar banner informativo
    BLOCKED_NEED_VIP  // Periodo gratuito cumplido: mostrar pantalla de bloqueo VIP
}

object VipAccessManager {
    private const val MILLIS_POR_DIA = 1000L * 60 * 60 * 24

    fun evaluateCompanyAccess(
        companyPlan: String,
        createdAtMillis: Long,
        nowMillis: Long,
        config: VipConfig
    ): AccessStatus {
        if (companyPlan == "VIP" || !config.isGateEnabled) {
            return AccessStatus.GRANTED
        }
        val elapsedDays = (nowMillis - createdAtMillis) / MILLIS_POR_DIA
        val remainingDays = config.freeTrialDays - elapsedDays

        return when {
            remainingDays <= 0 && config.isHardBlock -> AccessStatus.BLOCKED_NEED_VIP
            remainingDays <= config.warningDaysBefore -> AccessStatus.EXPIRING_SOON
            else -> AccessStatus.TRIAL_ACTIVE
        }
    }

    fun diasRestantes(createdAtMillis: Long, nowMillis: Long, config: VipConfig): Long =
        config.freeTrialDays - (nowMillis - createdAtMillis) / MILLIS_POR_DIA
}
