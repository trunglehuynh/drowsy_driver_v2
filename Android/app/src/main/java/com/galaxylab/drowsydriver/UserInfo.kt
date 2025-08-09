package com.galaxylab.drowsydriver

import android.content.SharedPreferences

class UserInfo(private val sharedPreferences: SharedPreferences) {

    companion object {
        const val AGREED_DISCLAIMER = "AGREED_DISCLAIMER"
        const val ALERT_EMPTY_FACE = "ALERT_EMPTY_FACE"

        const val SP_SENSITIVE_THRESHOLD_ALERT = "SP_SENSITIVE_THRESHOLD_ALERT"
        const val DEFAULT_SENSITIVE_THRESHOLD_ALERT = 90
        const val MIN_SENSITIVE_THRESHOLD_ALERT = 10
        const val MAX_SENSITIVE_THRESHOLD_ALERT = 95

        const val SP_DURATION_THRESHOLD_ALERT = "SP_DURATION_THRESHOLD_ALERT"
        const val DEFAULT_DURATION_THRESHOLD_ALERT = 500L
        const val MIN_DURATION_THRESHOLD_ALERT = 100L
        const val MAX_DURATION_THRESHOLD_ALERT = 5000L

        const val INIT_INSTALL_VERSION_KEY =
            "INIT_INSTALL_VERSION" // the first version user install app

        // all users install app version after 17 are required to pay
        const val MAX_FREE_VERSION_NUMBER = 17
        const val DEFAULT_EMPTY_FACE_ALERT = false
    }

    fun isUserAgreedDisclaimer() = sharedPreferences.getBoolean(AGREED_DISCLAIMER, false)
    fun userAgreedDisclaimer() =
        sharedPreferences.edit().putBoolean(AGREED_DISCLAIMER, true).commit()

    fun isAlertEmptyFace() = sharedPreferences.getBoolean(ALERT_EMPTY_FACE, DEFAULT_EMPTY_FACE_ALERT)
    fun updateAlertEmptyFace(isAlert: Boolean) =
        sharedPreferences.edit().putBoolean(ALERT_EMPTY_FACE, isAlert).commit()

    // Sensitive Threshold
    fun getSensitiveThreshold(): Int =
        sharedPreferences.getInt(SP_SENSITIVE_THRESHOLD_ALERT, DEFAULT_SENSITIVE_THRESHOLD_ALERT)

    fun updateSensitiveThreshold(value: Int): Boolean {
        val clamped = value.coerceIn(MIN_SENSITIVE_THRESHOLD_ALERT, MAX_SENSITIVE_THRESHOLD_ALERT)
        return sharedPreferences.edit().putInt(SP_SENSITIVE_THRESHOLD_ALERT, clamped).commit()
    }

    // Duration Threshold
    fun getDurationThreshold(): Long =
        sharedPreferences.getLong(SP_DURATION_THRESHOLD_ALERT, DEFAULT_DURATION_THRESHOLD_ALERT)

    fun updateDurationThreshold(value: Long): Boolean {
        val clamped = value.coerceIn(MIN_DURATION_THRESHOLD_ALERT, MAX_DURATION_THRESHOLD_ALERT)
        return sharedPreferences.edit().putLong(SP_DURATION_THRESHOLD_ALERT, clamped).commit()
    }

    fun mayUpdateInitialInstallVersion() {
        val currentVersion = getInitialInstallVersion()
        if (currentVersion != -1) return
        sharedPreferences.edit().putInt(INIT_INSTALL_VERSION_KEY, BuildConfig.VERSION_CODE).apply()
    }

    fun getInitialInstallVersion(): Int = sharedPreferences.getInt(INIT_INSTALL_VERSION_KEY, -1)

    fun shouldShowPaywallForPreviousUsers(): Boolean =
        getInitialInstallVersion() > MAX_FREE_VERSION_NUMBER
}