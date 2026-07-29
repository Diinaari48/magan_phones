package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.preferences.NotificationPreferences
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    preferences: NotificationPreferences,
    onDismiss: () -> Unit
) {
    var masterEnabled by remember { mutableStateOf(preferences.notificationsEnabled) }
    var t50 by remember { mutableStateOf(preferences.threshold50Enabled) }
    var t45 by remember { mutableStateOf(preferences.threshold45Enabled) }
    var t40 by remember { mutableStateOf(preferences.threshold40Enabled) }
    var offlineAlerts by remember { mutableStateOf(preferences.offlineAlertsEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_dialog_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = CyanAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Notification Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Battery threshold & status alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = {
                            masterEnabled = it
                            preferences.notificationsEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Alert Threshold Levels",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                ThresholdCheckboxItem(
                    label = "50% Battery Warning",
                    checked = t50,
                    enabled = masterEnabled,
                    onCheckedChange = {
                        t50 = it
                        preferences.threshold50Enabled = it
                    }
                )

                ThresholdCheckboxItem(
                    label = "45% Battery Warning",
                    checked = t45,
                    enabled = masterEnabled,
                    onCheckedChange = {
                        t45 = it
                        preferences.threshold45Enabled = it
                    }
                )

                ThresholdCheckboxItem(
                    label = "40% Low Battery & 5-min Repeating Alert",
                    checked = t40,
                    enabled = masterEnabled,
                    onCheckedChange = {
                        t40 = it
                        preferences.threshold40Enabled = it
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Device Connection Alerts",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                ThresholdCheckboxItem(
                    label = "Offline / Online Status Alerts (90s timeout)",
                    checked = offlineAlerts,
                    enabled = masterEnabled,
                    onCheckedChange = {
                        offlineAlerts = it
                        preferences.offlineAlertsEnabled = it
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = CyanAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ThresholdCheckboxItem(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = CyanPrimary,
                uncheckedColor = SlateCardBorder
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) TextPrimary else TextMuted
        )
    }
}
