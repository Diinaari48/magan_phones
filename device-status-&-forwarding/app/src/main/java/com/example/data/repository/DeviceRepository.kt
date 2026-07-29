package com.example.data.repository

import android.content.Context
import com.example.data.SupabaseClientManager
import com.example.data.models.DeviceStatus
import com.example.data.models.ForwardingCommand
import com.example.data.preferences.NotificationPreferences
import com.example.monitoring.NotificationHelper
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class DeviceRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = NotificationPreferences(context)

    private val _devices = MutableStateFlow<List<DeviceStatus>>(emptyList())
    val devices: StateFlow<List<DeviceStatus>> = _devices.asStateFlow()

    private val _commands = MutableStateFlow<List<ForwardingCommand>>(emptyList())
    val commands: StateFlow<List<ForwardingCommand>> = _commands.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var isSubscribedToRealtime = false

    init {
        refreshAllData()
        startRealtimeSubscriptions()
    }

    fun refreshAllData() {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                fetchDevicesInternal()
                fetchCommandsInternal()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to query database"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchDevicesInternal() {
        val client = SupabaseClientManager.client
        if (client != null) {
            try {
                val result = client.postgrest["device_status"]
                    .select()
                    .decodeList<DeviceStatus>()
                
                _devices.value = result
                checkBatteryAndOfflineEvents(result)
            } catch (e: Exception) {
                e.printStackTrace()
                // If query fails, fall back to mock data so UI displays gracefully
                if (_devices.value.isEmpty()) {
                    loadMockDevices()
                }
            }
        } else {
            loadMockDevices()
        }
    }

    private suspend fun fetchCommandsInternal() {
        val client = SupabaseClientManager.client
        if (client != null) {
            try {
                val result = client.postgrest["commands"]
                    .select {
                        order("created_at", order = Order.DESCENDING)
                        limit(10)
                    }
                    .decodeList<ForwardingCommand>()
                _commands.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                if (_commands.value.isEmpty()) {
                    loadMockCommands()
                }
            }
        } else {
            loadMockCommands()
        }
    }

    private fun startRealtimeSubscriptions() {
        val client = SupabaseClientManager.client
        if (client == null || isSubscribedToRealtime) return

        isSubscribedToRealtime = true
        scope.launch {
            try {
                val channel = client.channel("device_status_changes")
                val deviceFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "device_status"
                }

                client.realtime.connect()
                channel.subscribe()

                deviceFlow.collect {
                    fetchDevicesInternal()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isSubscribedToRealtime = false
            }
        }

        scope.launch {
            try {
                val channel = client.channel("commands_changes")
                val commandFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "commands"
                }
                channel.subscribe()

                commandFlow.collect {
                    fetchCommandsInternal()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun sendForwardingCommand(targetNumber: String, deviceId: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val client = SupabaseClientManager.client
            val nowIso = Instant.now().toString()

            val newCommand = ForwardingCommand(
                targetNumber = targetNumber,
                status = "pending",
                deviceId = deviceId,
                createdAt = nowIso
            )

            if (client != null) {
                try {
                    client.postgrest["commands"].insert(newCommand)
                    fetchCommandsInternal()
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            } else {
                // Mock execution for offline/demo environment
                val updatedList = listOf(
                    newCommand.copy(id = "cmd_${System.currentTimeMillis()}")
                ) + _commands.value.take(9)
                _commands.value = updatedList
                
                // Simulate status progression from pending -> sent after 3 seconds
                scope.launch {
                    delay(3000)
                    _commands.value = _commands.value.map { cmd ->
                        if (cmd.id == newCommand.id || (cmd.targetNumber == targetNumber && cmd.status == "pending")) {
                            cmd.copy(status = "sent")
                        } else cmd
                    }
                    // Also update target device forwarding_enabled status to true
                    _devices.value = _devices.value.map { dev ->
                        if (deviceId == null || dev.deviceId == deviceId) {
                            dev.copy(forwardingEnabled = true)
                        } else dev
                    }
                }
                Result.success(Unit)
            }
        }
    }

    private fun checkBatteryAndOfflineEvents(newList: List<DeviceStatus>) {
        for (device in newList) {
            NotificationHelper.checkAndNotifyDeviceStatusChange(context, device, prefs)
        }
    }

    private fun loadMockDevices() {
        val now = Instant.now()
        val mockList = listOf(
            DeviceStatus(
                deviceId = "dev_001",
                deviceModel = "Pixel 8 Pro",
                batteryLevel = 68,
                forwardingEnabled = true,
                lastSeen = now.minusSeconds(15).toString(),
                connectionType = "wifi",
                chargingStatus = "charging"
            ),
            DeviceStatus(
                deviceId = "dev_002",
                deviceModel = "Galaxy S24 Ultra",
                batteryLevel = 38,
                forwardingEnabled = false,
                lastSeen = now.minusSeconds(42).toString(),
                connectionType = "mobile",
                chargingStatus = "not_charging"
            ),
            DeviceStatus(
                deviceId = "dev_003",
                deviceModel = "Xperia 1 V",
                batteryLevel = 18,
                forwardingEnabled = true,
                lastSeen = now.minusSeconds(110).toString(),
                connectionType = "wifi",
                chargingStatus = "not_charging"
            ),
            DeviceStatus(
                deviceId = "dev_004",
                deviceModel = "OnePlus 12",
                batteryLevel = 8,
                forwardingEnabled = false,
                lastSeen = now.minusSeconds(8).toString(),
                connectionType = "none",
                chargingStatus = "charging"
            )
        )
        _devices.value = mockList
        checkBatteryAndOfflineEvents(mockList)
    }

    private fun loadMockCommands() {
        val now = Instant.now()
        _commands.value = listOf(
            ForwardingCommand(
                id = "cmd_101",
                targetNumber = "+15550192834",
                status = "sent",
                deviceId = "dev_001",
                deviceModel = "Pixel 8 Pro",
                createdAt = now.minusSeconds(300).toString()
            ),
            ForwardingCommand(
                id = "cmd_102",
                targetNumber = "+15550189922",
                status = "pending",
                deviceId = "dev_002",
                deviceModel = "Galaxy S24 Ultra",
                createdAt = now.minusSeconds(45).toString()
            )
        )
    }
}
