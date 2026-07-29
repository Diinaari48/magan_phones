package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("device_monitor_prefs", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var threshold50Enabled: Boolean
        get() = prefs.getBoolean("threshold_50", true)
        set(value) = prefs.edit().putBoolean("threshold_50", value).apply()

    var threshold45Enabled: Boolean
        get() = prefs.getBoolean("threshold_45", true)
        set(value) = prefs.edit().putBoolean("threshold_45", value).apply()

    var threshold40Enabled: Boolean
        get() = prefs.getBoolean("threshold_40", true)
        set(value) = prefs.edit().putBoolean("threshold_40", value).apply()

    var offlineAlertsEnabled: Boolean
        get() = prefs.getBoolean("offline_alerts", true)
        set(value) = prefs.edit().putBoolean("offline_alerts", value).apply()

    fun getLastNotifiedThreshold(deviceId: String): Int {
        return prefs.getInt("last_threshold_$deviceId", 101)
    }

    fun setLastNotifiedThreshold(deviceId: String, threshold: Int) {
        prefs.edit().putInt("last_threshold_$deviceId", threshold).apply()
    }

    fun getLastRepeatingAlertTime(deviceId: String): Long {
        return prefs.getLong("last_repeating_alert_$deviceId", 0L)
    }

    fun setLastRepeatingAlertTime(deviceId: String, timestamp: Long) {
        prefs.edit().putLong("last_repeating_alert_$deviceId", timestamp).apply()
    }

    fun isDeviceNotifiedOffline(deviceId: String): Boolean {
        return prefs.getBoolean("offline_state_$deviceId", false)
    }

    fun setDeviceNotifiedOffline(deviceId: String, isOffline: Boolean) {
        prefs.edit().putBoolean("offline_state_$deviceId", isOffline).apply()
    }
}
