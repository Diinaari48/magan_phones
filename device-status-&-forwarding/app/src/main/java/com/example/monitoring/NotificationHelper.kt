package com.example.monitoring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.models.DeviceStatus
import com.example.data.preferences.NotificationPreferences
import java.time.Instant

object NotificationHelper {
    const val CHANNEL_HIGH_ID = "device_battery_high_channel"
    const val CHANNEL_DEFAULT_ID = "device_battery_default_channel"
    const val CHANNEL_SERVICE_ID = "device_monitor_service_channel"

    private const val CHANNEL_HIGH_NAME = "Critical Battery & Status Alerts"
    private const val CHANNEL_DEFAULT_NAME = "Battery Warning Alerts"
    private const val CHANNEL_SERVICE_NAME = "Device Monitoring Service"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val highChannel = NotificationChannel(
                CHANNEL_HIGH_ID,
                CHANNEL_HIGH_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for low battery (<=20%) and offline status changes"
                enableVibration(true)
            }

            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT_ID,
                CHANNEL_DEFAULT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard notifications for 40% battery threshold warnings"
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                CHANNEL_SERVICE_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for background device monitoring"
            }

            notificationManager.createNotificationChannel(highChannel)
            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun checkAndNotifyDeviceStatusChange(
        context: Context,
        device: DeviceStatus,
        prefs: NotificationPreferences
    ) {
        if (!prefs.notificationsEnabled) return

        val deviceName = device.deviceModel?.ifBlank { null } ?: device.deviceId
        val battery = device.batteryLevel
        val nowMs = System.currentTimeMillis()

        // 1. Check battery thresholds (50%, 45%, 40%) & repeating low battery (<= 40%)
        if (battery != null) {
            val lastNotified = prefs.getLastNotifiedThreshold(device.deviceId)
            val crossedThreshold = determineCrossedThreshold(battery, lastNotified, prefs)

            if (crossedThreshold != null) {
                val isHighPriority = crossedThreshold <= 40
                val channelId = if (isHighPriority) CHANNEL_HIGH_ID else CHANNEL_DEFAULT_ID

                fireNotification(
                    context = context,
                    notificationId = device.deviceId.hashCode() + crossedThreshold,
                    channelId = channelId,
                    title = "$deviceName battery alert",
                    body = "$deviceName battery is at $battery%",
                    isHighPriority = isHighPriority
                )

                prefs.setLastNotifiedThreshold(device.deviceId, crossedThreshold)
                prefs.setLastRepeatingAlertTime(device.deviceId, nowMs)
            } else if (battery <= 40) {
                // Repeating low-battery alert every 5 minutes while <= 40%
                val lastRepeating = prefs.getLastRepeatingAlertTime(device.deviceId)
                if (nowMs - lastRepeating >= 5 * 60 * 1000L) {
                    fireNotification(
                        context = context,
                        notificationId = device.deviceId.hashCode() + 400,
                        channelId = CHANNEL_HIGH_ID,
                        title = "$deviceName battery alert",
                        body = "$deviceName battery is at $battery%",
                        isHighPriority = true
                    )
                    prefs.setLastRepeatingAlertTime(device.deviceId, nowMs)
                }
            } else if (battery > 40) {
                // Reset threshold & repeating alert tracking when battery rises above 40%
                if (lastNotified < 101 || prefs.getLastRepeatingAlertTime(device.deviceId) > 0L) {
                    prefs.setLastNotifiedThreshold(device.deviceId, 101)
                    prefs.setLastRepeatingAlertTime(device.deviceId, 0L)
                }
            }
        }

        // 2. Check Online/Offline status crossing
        if (prefs.offlineAlertsEnabled) {
            val isCurrentlyOffline = isDeviceOffline(device.lastSeen)
            val storedOfflineState = prefs.isDeviceNotifiedOffline(device.deviceId)

            if (isCurrentlyOffline && !storedOfflineState) {
                // Device went offline
                prefs.setDeviceNotifiedOffline(device.deviceId, true)
                fireNotification(
                    context = context,
                    notificationId = device.deviceId.hashCode() + 999,
                    channelId = CHANNEL_HIGH_ID,
                    title = "$deviceName Offline",
                    body = "$deviceName stopped responding (no heartbeat for 90+ seconds)",
                    isHighPriority = true
                )
            } else if (!isCurrentlyOffline && storedOfflineState) {
                // Device came back online
                prefs.setDeviceNotifiedOffline(device.deviceId, false)
                fireNotification(
                    context = context,
                    notificationId = device.deviceId.hashCode() + 888,
                    channelId = CHANNEL_HIGH_ID,
                    title = "$deviceName Online",
                    body = "$deviceName is back online and actively sending status updates",
                    isHighPriority = false
                )
            }
        }
    }

    private fun determineCrossedThreshold(
        currentBattery: Int,
        lastNotifiedThreshold: Int,
        prefs: NotificationPreferences
    ): Int? {
        val thresholds = listOf(
            50 to prefs.threshold50Enabled,
            45 to prefs.threshold45Enabled,
            40 to prefs.threshold40Enabled
        )

        for ((level, enabled) in thresholds) {
            if (enabled && currentBattery <= level && lastNotifiedThreshold > level) {
                return level
            }
        }
        return null
    }

    fun isDeviceOffline(lastSeenIso: String?): Boolean {
        if (lastSeenIso.isNullOrBlank()) return true
        return try {
            val epochSec = parseIsoToEpochSeconds(lastSeenIso)
            val currentEpoch = Instant.now().epochSecond
            (currentEpoch - epochSec) > 90
        } catch (e: Exception) {
            false
        }
    }

    fun parseIsoToEpochSeconds(isoString: String): Long {
        return try {
            Instant.parse(isoString).epochSecond
        } catch (e: Exception) {
            try {
                // Fallback for custom formatted timestamps or epoch ms
                isoString.toLongOrNull()?.let { if (it > 10_000_000_000L) it / 1000 else it }
                    ?: Instant.now().epochSecond
            } catch (e2: Exception) {
                Instant.now().epochSecond
            }
        }
    }

    private fun fireNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        isHighPriority: Boolean
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
