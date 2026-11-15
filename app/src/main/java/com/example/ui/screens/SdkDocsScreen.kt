package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SdkEventLog
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
fun SdkDocsScreen(
    sdkLogs: List<SdkEventLog>,
    onInstallAndLaunchDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current

    val sdkSnippet = """
        <script src="/sdk/motionx-sdk.js"></script>
        <script>
          MotionX.onHandMove((data) => {
            // data.leftHand & data.rightHand (21 landmarks each)
            console.log(data.leftHand, data.rightHand);
          });

          MotionX.onGesture((gesture) => {
            // gesture.name, gesture.hand, gesture.mappedControl
            console.log(gesture.name);
          });

          MotionX.onAirDraw((drawing) => {
            console.log(drawing.points);
          });

          MotionX.onObjectAction((obj) => {
            // Virtual weapons & GLB/glTF objects placed from library
            console.log(obj.name, obj.position, obj.rotation, obj.scale);
          });

          MotionX.onConnection((status) => {
            console.log(status);
          });
        </script>
    """.trimIndent()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("sdk_docs_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "MotionX JavaScript SDK v2.0",
                subtitle = "Build custom HTML5 / WebGL / Three.js motion-controlled games"
            )
        }

        item {
            GlassPanel(highlighted = true, accentColor = ElectricCyan) {
                Text(
                    text = "Zero-Setup Runtime Injection",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Every uploaded HTML/ZIP game receives window.MotionX automatically inside both the phone Preview and the Wi-Fi TV Receiver.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onInstallAndLaunchDemo,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("launch_sdk_arena_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Launch Cyber Motion Arena (Working SDK Example)", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            GlassPanel(accentColor = NeonBlue) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = ElectricCyan)
                        Text("Quick Integration Example", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(sdkSnippet)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy SDK snippet", tint = ElectricCyan)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sdkSnippet,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PitchBlack)
                        .padding(12.dp)
                )
            }
        }

        item {
            GlassPanel(accentColor = CyberPurple) {
                Text("API Reference", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val methods = listOf(
                    "MotionX.onHandMove(cb)" to "Streams 21 landmarks/hand, palmCenter, palmRoll, fingerBends, pinchDistance & handsDistance.",
                    "MotionX.onGesture(cb)" to "Fires mapped gestures (PINCH, CLOSED_FIST, OPEN_PALM, MOVE_LEFT/RIGHT, RAISE/LOWER, PUSH/PULL).",
                    "MotionX.onAirDraw(cb)" to "Delivers real-time 3D index-finger stroke points, activeColor & brushSize.",
                    "MotionX.onObjectAction(cb)" to "Delivers placed 3D weapons/objects & live hand transform matrices.",
                    "MotionX.onConnection(cb)" to "Reports pairing status, deviceName, PIN, latencyMs & transport channel.",
                    "MotionX.setMapping(obj)" to "Allows custom games to override default key/action mapping profiles."
                )
                methods.forEach { (sig, desc) ->
                    Text(sig, style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        item {
            SectionHeader(title = "Live SDK Event Stream (${sdkLogs.size})")
        }

        if (sdkLogs.isEmpty()) {
            item {
                GlassPanel(accentColor = CoolSlate) {
                    Text(
                        "No live SDK events captured yet. Run tracking or test gestures in Game Preview to see live JSON dispatches.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate
                    )
                }
            }
        } else {
            items(sdkLogs, key = { it.id }) { log ->
                GlassPanel(accentColor = EmeraldSignal, contentPadding = 10.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(label = log.channel.uppercase(), color = EmeraldSignal)
                        Text(log.payloadPreview, style = MaterialTheme.typography.labelSmall, color = PureWhite)
                    }
                }
            }
        }
    }
}
