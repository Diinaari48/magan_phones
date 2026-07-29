package com.example.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForwardingCommand(
    @SerialName("id") val id: String? = null,
    @SerialName("target_number") val targetNumber: String,
    @SerialName("status") val status: String = "pending",
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
