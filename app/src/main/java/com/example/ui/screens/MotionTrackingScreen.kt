package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.CameraResolutionPreset
import com.example.model.DualHandTrackingFrame
import com.example.model.HandTelemetry
import com.example.model.MotionSettingsState
import com.example.ui.theme.AmberWarn
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
import com.example.vision.GestureRecognizer

@Composable
fun MotionTrackingScreen(
    trackingFrame: DualHandTrackingFrame,
    trackingError: String?,
    settings: MotionSettingsState,
    onStartCamera: (LifecycleOwner, PreviewView) -> Unit,
    onStopCamera: () -> Unit,
    onToggleLens: (Boolean) -> Unit,
    onSelectResolution: (CameraResolutionPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            previewViewRef?.let { onStartCamera(lifecycleOwner, it) }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopCamera()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("motion_tracking_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "AI Dual-Hand Tracking",
                subtitle = "42-landmark local MediaPipe vision • Zero video upload"
            )
        }

        // Camera Preview + Dual Hand Skeleton Overlay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PitchBlack)
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    HandSkeletonOverlay(
                        frame = trackingFrame,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Explicit Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.camera_permission_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack),
                            modifier = Modifier.testTag("grant_camera_button")
                        ) {
                            Text("Enable Hand Tracking Camera", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Clear camera & tracking status HUD badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(
                        label = if (trackingFrame.isCameraActive) stringResource(R.string.camera_active_badge) else "CAMERA OFF",
                        color = if (trackingFrame.isCameraActive) EmeraldSignal else CoolSlate
                    )
                    if (trackingFrame.isLowLight) {
                        StatusPill(label = "LOW LIGHT", color = AmberWarn)
                    }
                }

                StatusPill(
                    label = "${trackingFrame.fps} FPS • ${trackingFrame.totalLandmarksDetected}/42 LM",
                    color = ElectricCyan,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }
        }

        // Start / Stop & Camera Lens Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            previewViewRef?.let { onStartCamera(lifecycleOwner, it) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_tracking_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Start Vision", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onStopCamera,
                    enabled = trackingFrame.isCameraActive,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stop_tracking_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = {
                        onToggleLens(!settings.useFrontCamera)
                        if (trackingFrame.isCameraActive) {
                            previewViewRef?.let { onStartCamera(lifecycleOwner, it) }
                        }
                    },
                    modifier = Modifier.testTag("switch_camera_lens_button")
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Front/Back Camera")
                }
            }
        }

        if (!trackingError.isNullOrBlank()) {
            item {
                GlassPanel(accentColor = AmberWarn) {
                    Text(trackingError, style = MaterialTheme.typography.bodyMedium, color = AmberWarn)
                }
            }
        }

        // Adaptive Resolution Presets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraResolutionPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = settings.resolutionPreset == preset,
                        onClick = {
                            onSelectResolution(preset)
                            if (trackingFrame.isCameraActive) {
                                previewViewRef?.let { onStartCamera(lifecycleOwner, it) }
                            }
                        },
                        label = {
                            Text(
                                when (preset) {
                                    CameraResolutionPreset.ECO -> "Eco 240p"
                                    CameraResolutionPreset.BALANCED -> "480p"
                                    CameraResolutionPreset.HIGH -> "720p"
                                }
                            )
                        }
                    )
                }
            }
        }

        // Dual-Hand Telemetry Breakdown
        item {
            GlassPanel(accentColor = NeonBlue) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(trackingFrame.trackingQualityHint, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text(
                        text = trackingFrame.handsDistance?.let { "Span: ${"%.2f".format(it)}" } ?: "Dual Span: --",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HandTelemetryCard("LEFT HAND (21 LM)", trackingFrame.leftHand, CyberPurple, Modifier.weight(1f))
                    HandTelemetryCard("RIGHT HAND (21 LM)", trackingFrame.rightHand, ElectricCyan, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HandSkeletonOverlay(
    frame: DualHandTrackingFrame,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        fun drawHand(hand: HandTelemetry?, color: Color) {
            if (hand == null || hand.landmarks.size < 21) return
            val pts = hand.landmarks.map { Offset(it.x * size.width, it.y * size.height) }
            GestureRecognizer.HAND_CONNECTIONS.forEach { (a, b) ->
                drawLine(
                    color = color.copy(alpha = 0.85f),
                    start = pts[a],
                    end = pts[b],
                    strokeWidth = 4f
                )
            }
            pts.forEachIndexed { idx, offset ->
                val radius = if (idx in listOf(0, 4, 8, 12, 16, 20)) 8f else 5f
                drawCircle(color = PureWhite, radius = radius + 2f, center = offset)
                drawCircle(color = color, radius = radius, center = offset)
            }
        }
        drawHand(frame.leftHand, CyberPurple)
        drawHand(frame.rightHand, ElectricCyan)
    }
}

@Composable
private fun HandTelemetryCard(
    title: String,
    hand: HandTelemetry?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PitchBlack.copy(alpha = 0.55f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = accent)
        Text(
            text = hand?.activeGesture?.displayName ?: "Not Detected",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite
        )
        Text(
            text = if (hand != null) "Roll: ${hand.palmRollDegrees.toInt()}° • Conf: ${(hand.confidence * 100).toInt()}%" else "Show hand",
            style = MaterialTheme.typography.labelSmall,
            color = CoolSlate
        )
        if (hand != null) {
            FingerBar("Thumb", hand.fingerBends.thumb, accent)
            FingerBar("Index", hand.fingerBends.index, accent)
            FingerBar("Middle", hand.fingerBends.middle, accent)
            FingerBar("Ring", hand.fingerBends.ring, accent)
            FingerBar("Pinky", hand.fingerBends.pinky, accent)
        }
    }
}

@Composable
private fun FingerBar(label: String, curl: Float, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label.take(1), style = MaterialTheme.typography.labelSmall, color = CoolSlate)
        LinearProgressIndicator(
            progress = { (1f - curl).coerceIn(0f, 1f) },
            color = accent,
            trackColor = CoolSlate.copy(alpha = 0.2f),
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
        )
    }
}
