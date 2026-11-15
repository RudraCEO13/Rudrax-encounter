package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CameraResolutionPreset
import com.example.model.ConnectedSessionInfo
import com.example.model.MotionSettingsState
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SectionHeader

@Composable
fun ConnectionSettingsScreen(
    settings: MotionSettingsState,
    sessionInfo: ConnectedSessionInfo,
    onUpdateSettings: ((MotionSettingsState) -> MotionSettingsState) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("connection_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Connection & Engine Settings",
                subtitle = "Optimize tracking performance, Wi-Fi/WebRTC transport & privacy"
            )
        }

        // Vision Engine Performance
        item {
            GlassPanel(accentColor = ElectricCyan) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricCyan)
                    Text("Vision & Smoothing Engine", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Adaptive Camera Resolution", style = MaterialTheme.typography.labelMedium, color = CoolSlate)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CameraResolutionPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.resolutionPreset == preset,
                            onClick = { onUpdateSettings { it.copy(resolutionPreset = preset) } },
                            label = { Text(preset.name) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "EMA Smoothing Alpha: ${"%.2f".format(settings.smoothingAlpha)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Slider(
                    value = settings.smoothingAlpha,
                    onValueChange = { v -> onUpdateSettings { it.copy(smoothingAlpha = v) } },
                    valueRange = 0.15f..0.90f
                )
                Text(
                    "Detection Confidence Threshold: ${(settings.minDetectionConfidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Slider(
                    value = settings.minDetectionConfidence,
                    onValueChange = { v -> onUpdateSettings { it.copy(minDetectionConfidence = v, minTrackingConfidence = v) } },
                    valueRange = 0.35f..0.85f
                )
                SettingToggleRow("Invert Horizontal (X) Axis", settings.invertXAxis) { v ->
                    onUpdateSettings { it.copy(invertXAxis = v) }
                }
                SettingToggleRow("Invert Vertical (Y) Axis", settings.invertYAxis) { v ->
                    onUpdateSettings { it.copy(invertYAxis = v) }
                }
            }
        }

        // Wireless Transport
        item {
            GlassPanel(accentColor = NeonBlue) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonBlue)
                    Text("Wi-Fi / WebRTC & Bluetooth Transport", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "HTTP Receiver Port: ${settings.httpPort} • WebSocket Stream Port: ${settings.wsPort}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Text(
                    "Active Endpoint: ${sessionInfo.wsStreamUrl.ifBlank { "Starting..." }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingToggleRow("WebRTC DataChannel Signaling", settings.enableWebRtcDataChannel) { v ->
                    onUpdateSettings { it.copy(enableWebRtcDataChannel = v) }
                }
                SettingToggleRow("Bluetooth Discovery & Pairing", settings.enableBluetoothDiscovery) { v ->
                    onUpdateSettings { it.copy(enableBluetoothDiscovery = v) }
                }
            }
        }

        // Privacy & Security
        item {
            GlassPanel(accentColor = EmeraldSignal) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSignal)
                    Text("Security & Local Privacy Guarantees", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val items = listOf(
                    "Camera frames are processed 100% locally on-device via MediaPipe.",
                    "Raw video footage is never recorded, stored, or streamed to TV/PC.",
                    "Only lightweight processed 21-landmark JSON coordinates transmit over PIN-authenticated channels.",
                    "Uploaded ZIP/HTML projects are Zip-Slip validated and sandboxed to loopback runtime."
                )
                items.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PureWhite)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
