package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.ConnectedSessionInfo
import com.example.model.ConnectionStatus
import com.example.model.CustomGameProject
import com.example.model.DualHandTrackingFrame
import com.example.model.GameRuntimeState
import com.example.model.GestureType
import com.example.model.HandSide
import com.example.model.TargetDeviceType
import com.example.runtime.MotionXRuntimeWebView
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill

@Composable
fun GamePreviewScreen(
    activeGame: CustomGameProject?,
    gameUrl: String,
    runtimeState: GameRuntimeState,
    previewOnExternalTv: Boolean,
    sessionInfo: ConnectedSessionInfo,
    trackingFrame: DualHandTrackingFrame,
    activeActionLabel: String,
    latestWebBridgeMessage: Pair<String, String>?,
    onStartGame: () -> Unit,
    onStopGame: () -> Unit,
    onToggleDisplayTarget: (Boolean) -> Unit,
    onPairWithPin: (String) -> Unit,
    onOpenTracking: () -> Unit,
    onOpenStudio: () -> Unit,
    onTestGesture: (GestureType, HandSide) -> Unit,
    onSdkFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickPin by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_preview_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Live Game Preview & TV Cast",
                subtitle = activeGame?.title ?: "Select or upload a custom game in Studio"
            )
        }

        // Control Bar: Start / Stop / Phone vs TV Display
        item {
            GlassPanel(accentColor = ElectricCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = runtimeState.label,
                        color = if (runtimeState == GameRuntimeState.RUNNING) EmeraldSignal else ElectricCyan
                    )
                    StatusPill(
                        label = if (trackingFrame.isCameraActive) "${trackingFrame.totalLandmarksDetected} LM LIVE" else "TRACKER IDLE",
                        color = if (trackingFrame.isCameraActive) EmeraldSignal else CyberPurple
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartGame,
                        enabled = activeGame != null,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSignal, contentColor = PitchBlack),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_game_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Start Game", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStopGame,
                        enabled = runtimeState == GameRuntimeState.RUNNING,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_game_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Stop")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !previewOnExternalTv,
                        onClick = { onToggleDisplayTarget(false) },
                        leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Phone Preview") }
                    )
                    FilterChip(
                        selected = previewOnExternalTv,
                        onClick = { onToggleDisplayTarget(true) },
                        leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("External TV Mode") }
                    )
                }
            }
        }

        // Actual Runtime Viewport
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PitchBlack)
            ) {
                if (activeGame == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No Custom Game Selected", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Upload your HTML/ZIP game or install the SDK Demo in Custom Game Studio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onOpenStudio) {
                            Text("Open Game Studio")
                        }
                    }
                } else if (previewOnExternalTv) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "TV / External Controller Mode",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Open on TV/Laptop: ${sessionInfo.localServerUrl}\nPIN: ${sessionInfo.pairingPin} • Clients Connected: ${sessionInfo.connectedClientsCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenTracking,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Open Camera Controller", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    MotionXRuntimeWebView(
                        gameUrl = gameUrl,
                        isRunning = runtimeState == GameRuntimeState.RUNNING,
                        latestChannelMessageJson = latestWebBridgeMessage,
                        onSdkFeedback = onSdkFeedback,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("game_webview_viewport")
                    )
                    if (runtimeState != GameRuntimeState.RUNNING) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PitchBlack.copy(alpha = 0.78f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onStartGame,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Run ${activeGame.title}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Motion Control Live Test Strip
        item {
            GlassPanel(accentColor = NeonBlue) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Motion Control Test Deck", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text(activeActionLabel, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        GestureType.MOVE_LEFT to "Move Left",
                        GestureType.MOVE_RIGHT to "Move Right",
                        GestureType.RAISE_HAND to "Jump",
                        GestureType.PINCH to "Fire / Select",
                        GestureType.CLOSED_FIST to "Grab",
                        GestureType.OPEN_PALM to "Release"
                    ).forEach { (gesture, label) ->
                        OutlinedButton(
                            onClick = { onTestGesture(gesture, HandSide.RIGHT) },
                            modifier = Modifier.testTag("test_gesture_${gesture.name.lowercase()}")
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        // Quick TV PIN Pair Bar
        item {
            GlassPanel(accentColor = CyberPurple) {
                Text(
                    text = "Connect Game to Paired TV / Laptop",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (sessionInfo.status == ConnectionStatus.CONNECTED) {
                        "Connected to ${sessionInfo.device?.name} • PIN ${sessionInfo.pairingPin}"
                    } else {
                        "Enter TV pairing PIN or open ${sessionInfo.localServerUrl}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickPin,
                        onValueChange = { quickPin = it.filter(Char::isDigit).take(6) },
                        label = { Text("Connection PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onPairWithPin(quickPin) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Icon(Icons.Default.CastConnected, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Pair")
                    }
                }
            }
        }
    }
}
