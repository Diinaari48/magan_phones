package com.example.monitoring

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.SupabaseClientManager
import com.example.data.models.DeviceStatus
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant

class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val deviceId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            val batteryLevel = getBatteryLevel(applicationContext)
            val chargingStatus = getChargingStatus(applicationContext)
            val connectionType = getConnectionType(applicationContext)
            val lastSeen = Instant.now().toString()

            val payload = DeviceStatus(
                deviceId = deviceId,
                deviceModel = deviceModel,
                batteryLevel = batteryLevel,
                chargingStatus = chargingStatus,
                connectionType = connectionType,
                lastSeen = lastSeen
            )

            Log.d("Heartbeat", payload.toString())

            val client = SupabaseClientManager.client
            if (client != null) {
                client.postgrest["device_status"].upsert(payload) {
                    onConflict = "device_id"
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("Heartbeat", "Error sending heartbeat", e)
            Result.retry()
        }
    }

    private fun getChargingStatus(context: Context): String {
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return if (isCharging) "charging" else "not_charging"
    }

    private fun getConnectionType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork ?: return "none"
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "none"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
            else -> "none"
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 0
    }
}
