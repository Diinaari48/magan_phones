package com.example.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceStatus(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("forwarding_enabled") val forwardingEnabled: Boolean? = false,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("connection_type") val connectionType: String? = null,
    @SerialName("charging_status") val chargingStatus: String? = null
)
