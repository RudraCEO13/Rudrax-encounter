package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ConnectedSessionInfo
import com.example.model.ConnectionStatus
import com.example.model.CustomGameProject
import com.example.model.DualHandTrackingFrame
import com.example.ui.navigation.MotionRoute
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.MetricChip
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PlasmaPink
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill

@Composable
fun DashboardScreen(
    sessionInfo: ConnectedSessionInfo,
    trackingFrame: DualHandTrackingFrame,
    activeGame: CustomGameProject?,
    gamesCount: Int,
    drawingsCount: Int,
    onNavigate: (MotionRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_motion_banner_1794724482867),
                    contentDescription = "RudraX Motion X Holographic Hand Interface",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PitchBlack.copy(alpha = 0.25f),
                                    PitchBlack.copy(alpha = 0.88f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                label = if (sessionInfo.status == ConnectionStatus.CONNECTED) {
                                    "TV PAIRED • ${sessionInfo.pairingPin}"
                                } else {
                                    "PIN ${sessionInfo.pairingPin}"
                                },
                                color = if (sessionInfo.status == ConnectionStatus.CONNECTED) EmeraldSignal else ElectricCyan
                            )
                            StatusPill(
                                label = if (trackingFrame.isCameraActive) "${trackingFrame.totalLandmarksDetected}/42 LM" else "VISION STANDBY",
                                color = if (trackingFrame.isCameraActive) ElectricCyan else CyberPurple
                            )
                        }
                        Text(
                            text = "RUDRAX MOTION X",
                            style = MaterialTheme.typography.displaySmall,
                            color = PureWhite
                        )
                        Text(
                            text = stringResource(R.string.brand_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = stringResource(R.string.studio_credit),
                            style = MaterialTheme.typography.labelSmall,
                            color = CoolSlate
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricChip(
                    icon = Icons.Default.CastConnected,
                    label = "DISPLAY LINK",
                    value = sessionInfo.device?.name ?: "Not Paired",
                    accent = if (sessionInfo.status == ConnectionStatus.CONNECTED) EmeraldSignal else ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    icon = Icons.Default.CameraAlt,
                    label = "HAND ENGINE",
                    value = if (trackingFrame.isCameraActive) "${trackingFrame.fps} FPS" else "Ready",
                    accent = CyberPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            GlassPanel(
                highlighted = true,
                accentColor = ElectricCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE RUNTIME PROJECT",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeGame?.title ?: "No Game Loaded Yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite
                        )
                        Text(
                            text = activeGame?.let { "${it.sourceType} • Entry: ${it.entryPoint}" }
                                ?: "Upload HTML / CSS / JS / ZIP or launch SDK Arena",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Button(
                        onClick = {
                            onNavigate(if (activeGame != null) MotionRoute.GamePreview else MotionRoute.GameStudio)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = PitchBlack
                        ),
                        modifier = Modifier.testTag("dashboard_launch_runtime_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(if (activeGame != null) "Launch" else "Upload", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Motion X Command Modules",
                subtitle = "All 10 studio, vision, display & SDK subsystems"
            )
        }

        item {
            val modules = listOf(
                ModuleTile("Device Pairing", "Bluetooth, QR, PIN & TV Wi-Fi", Icons.Default.CastConnected, ElectricCyan, MotionRoute.DeviceConnection),
                ModuleTile("AI Hand Tracker", "Dual-hand 42 landmarks & gestures", Icons.Default.CameraAlt, NeonBlue, MotionRoute.MotionTracking),
                ModuleTile("Custom Game Studio", "$gamesCount games • HTML/ZIP/GLB", Icons.Default.UploadFile, CyberPurple, MotionRoute.GameStudio),
                ModuleTile("Game Preview & TV", "Live runtime & external screen", Icons.Default.Gamepad, EmeraldSignal, MotionRoute.GamePreview),
                ModuleTile("Air Drawing 3D", "$drawingsCount saved • OBJ/glTF export", Icons.Default.Brush, PlasmaPink, MotionRoute.AirDrawing),
                ModuleTile("3D Weapon Library", "AK-47, Katana, GLB & hand-place", Icons.Default.ViewInAr, ElectricCyan, MotionRoute.ObjectLibrary),
                ModuleTile("Gesture Mapping", "Left & Right hand control studio", Icons.Default.Tune, NeonBlue, MotionRoute.GestureConfig),
                ModuleTile("Connection Settings", "WebRTC, Wi-Fi ports & privacy", Icons.Default.Settings, CoolSlate, MotionRoute.ConnectionSettings),
                ModuleTile("MotionX JS SDK", "API docs & live payload inspector", Icons.AutoMirrored.Filled.MenuBook, CyberPurple, MotionRoute.SdkDocs)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                modules.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { tile ->
                            GlassPanel(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tile_${tile.route.route}"),
                                accentColor = tile.accent,
                                onClick = { onNavigate(tile.route) },
                                contentPadding = 14.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tile.accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title,
                                        tint = tile.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tile.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private data class ModuleTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val route: MotionRoute
)
