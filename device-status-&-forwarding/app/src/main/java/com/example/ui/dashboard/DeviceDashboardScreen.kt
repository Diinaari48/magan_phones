package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DeviceStatus
import com.example.monitoring.NotificationHelper
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedAlert
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun DeviceDashboardScreen(
    devices: List<DeviceStatus>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ticking timestamp state to update relative time live every second
    var currentTimeSec by remember { mutableLongStateOf(Instant.now().epochSecond) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeSec = Instant.now().epochSecond
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live Device Monitor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${devices.size} device(s) connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SlateCard)
                        .testTag("refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Devices",
                        tint = CyanAccent
                    )
                }
            }

            if (isLoading && devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            } else if (error != null && devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = RedAlert,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connection Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Retry Connection", color = SlateDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No devices found yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connected devices running the status service will appear here in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = devices,
                        key = { it.deviceId }
                    ) { device ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            DeviceCard(
                                device = device,
                                currentTimeSec = currentTimeSec
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: DeviceStatus,
    currentTimeSec: Long
) {
    val battery = device.batteryLevel ?: 0
    val isOffline = NotificationHelper.isDeviceOffline(device.lastSeen)

    val relativeTimeText = remember(device.lastSeen, currentTimeSec) {
        formatRelativeTime(device.lastSeen, currentTimeSec)
    }

    val batteryColor by animateColorAsState(
        targetValue = when {
            battery > 50 -> GreenSuccess
            battery in 20..50 -> AmberWarning
            else -> RedAlert
        },
        animationSpec = tween(durationMillis = 500),
        label = "batteryColorAnimation"
    )

    val animatedBatteryProgress by animateFloatAsState(
        targetValue = (battery / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "batteryProgressAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.deviceId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Model name, Online indicator dot & Relative Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOffline) RedAlert else GreenSuccess)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = device.deviceModel?.ifBlank { null } ?: device.deviceId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOffline) RedAlert.copy(alpha = 0.15f) else GreenSuccess.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOffline) Icons.Default.SignalCellularOff else Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = if (isOffline) RedAlert else GreenSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = relativeTimeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOffline) RedAlert else GreenSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Middle Row: Circular/Horizontal Gauge & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    Canvas(modifier = Modifier.size(54.dp)) {
                        // Track background
                        drawArc(
                            color = SlateCardBorder,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Progress arc
                        drawArc(
                            color = batteryColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedBatteryProgress,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$battery%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = batteryColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Linear Gauge detail
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Battery Level",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (device.chargingStatus == "charging") {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Charging",
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            device.connectionType?.let { conn ->
                                Spacer(modifier = Modifier.width(6.dp))
                                val (connIcon, connColor) = when (conn.lowercase()) {
                                    "wifi" -> Icons.Default.Wifi to CyanAccent
                                    "mobile" -> Icons.Default.SignalCellularAlt to CyanAccent
                                    else -> Icons.Default.WifiOff to TextMuted
                                }
                                Icon(
                                    imageVector = connIcon,
                                    contentDescription = "Connection: $conn",
                                    tint = connColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                battery > 50 -> "Good"
                                battery in 20..50 -> "Moderate"
                                else -> "LOW BATTERY"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = batteryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedBatteryProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = batteryColor,
                        trackColor = SlateDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Row: Forwarding Badge & Device ID label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isForwarding = device.forwardingEnabled == true
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isForwarding) GreenSuccess.copy(alpha = 0.2f) else SlateDark,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isForwarding) GreenSuccess else SlateCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isForwarding) Icons.Default.Call else Icons.Default.CallEnd,
                            contentDescription = null,
                            tint = if (isForwarding) GreenSuccess else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isForwarding) "Forwarding: ON" else "Forwarding: OFF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isForwarding) GreenSuccess else TextSecondary
                        )
                    }
                }

                Text(
                    text = "ID: ${device.deviceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

private fun formatRelativeTime(lastSeenIso: String?, currentTimeSec: Long): String {
    if (lastSeenIso.isNullOrBlank()) return "Offline"
    val epochSec = NotificationHelper.parseIsoToEpochSeconds(lastSeenIso)
    val diffSec = (currentTimeSec - epochSec).coerceAtLeast(0)

    return when {
        diffSec < 5 -> "Just now"
        diffSec < 60 -> "Last seen ${diffSec}s ago"
        diffSec < 3600 -> "Last seen ${diffSec / 60}m ago"
        else -> "Last seen ${diffSec / 3600}h ago"
    }
}
