package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AirDrawingPalette
import com.example.model.AirPoint3D
import com.example.model.AirShapeTemplate
import com.example.model.AirStroke3D
import com.example.model.DualHandTrackingFrame
import com.example.model.HandSide
import com.example.model.SavedAirDrawing
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AirDrawingScreen(
    strokes: List<AirStroke3D>,
    canRedo: Boolean,
    activeHand: HandSide,
    is3DMode: Boolean,
    activeColorArgb: Long,
    brushSize: Float,
    fingerTrackingEnabled: Boolean,
    trackingFrame: DualHandTrackingFrame,
    savedDrawings: List<SavedAirDrawing>,
    onSelectHand: (HandSide) -> Unit,
    onToggle3DMode: (Boolean) -> Unit,
    onSelectColor: (Long) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onToggleFingerTracking: (Boolean) -> Unit,
    onTouchDrawPoint: (Float, Float, Boolean) -> Unit,
    onEndStroke: () -> Unit,
    onInsertShapePreset: (AirShapeTemplate) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onSaveAndExport: (String) -> Unit,
    onShareExport: (String?) -> Unit,
    onDeleteSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var artworkTitle by remember { mutableStateOf("Cyber Blade") }
    var orbitMode by remember { mutableStateOf(false) }
    var orbitYaw by remember { mutableFloatStateOf(0f) }
    var orbitPitch by remember { mutableFloatStateOf(0f) }

    val activeFinger = if (activeHand == HandSide.RIGHT) trackingFrame.rightHand else trackingFrame.leftHand

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("air_drawing_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "3D Air Drawing Studio",
                subtitle = "Draw shapes & weapons in mid-air with your index finger or touch"
            )
        }

        // Top Mode Controls
        item {
            GlassPanel(accentColor = ElectricCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = activeHand == HandSide.RIGHT,
                            onClick = { onSelectHand(HandSide.RIGHT) },
                            label = { Text("Right Index") }
                        )
                        FilterChip(
                            selected = activeHand == HandSide.LEFT,
                            onClick = { onSelectHand(HandSide.LEFT) },
                            label = { Text("Left Index") }
                        )
                    }
                    StatusPill(
                        label = if (activeFinger?.isIndexPointing == true || activeFinger?.isPinching == true) {
                            "DRAWING LIVE"
                        } else if (trackingFrame.isCameraActive) {
                            "POINT TO DRAW"
                        } else {
                            "TOUCH / READY"
                        },
                        color = if (activeFinger?.isIndexPointing == true) EmeraldSignal else ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("3D Depth Mode", style = MaterialTheme.typography.bodyMedium, color = PureWhite)
                        Switch(checked = is3DMode, onCheckedChange = onToggle3DMode)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Orbit 3D", style = MaterialTheme.typography.bodyMedium, color = PureWhite)
                        Switch(checked = orbitMode, onCheckedChange = { orbitMode = it })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Air Finger Tracking Input", style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                    Switch(checked = fingerTrackingEnabled, onCheckedChange = onToggleFingerTracking)
                }
            }
        }

        // 3D Air Drawing Canvas
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF04070E))
                    .border(BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
                    .pointerInput(orbitMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (!orbitMode) {
                                    onTouchDrawPoint(
                                        (offset.x / size.width).coerceIn(0f, 1f),
                                        (offset.y / size.height).coerceIn(0f, 1f),
                                        true
                                    )
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (orbitMode) {
                                    orbitYaw += dragAmount.x * 0.45f
                                    orbitPitch = (orbitPitch + dragAmount.y * 0.35f).coerceIn(-75f, 75f)
                                } else {
                                    onTouchDrawPoint(
                                        (change.position.x / size.width).coerceIn(0f, 1f),
                                        (change.position.y / size.height).coerceIn(0f, 1f),
                                        false
                                    )
                                }
                            },
                            onDragEnd = {
                                if (!orbitMode) onEndStroke()
                            }
                        )
                    }
                    .testTag("air_drawing_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Perspective floor grid
                    val gridColor = ElectricCyan.copy(alpha = 0.08f)
                    val cols = 8
                    for (i in 0..cols) {
                        val x = size.width * (i / cols.toFloat())
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        val y = size.height * (i / cols.toFloat())
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }

                    val yawRad = Math.toRadians(orbitYaw.toDouble())
                    val pitchRad = Math.toRadians(orbitPitch.toDouble())
                    val cosY = cos(yawRad).toFloat()
                    val sinY = sin(yawRad).toFloat()
                    val cosP = cos(pitchRad).toFloat()
                    val sinP = sin(pitchRad).toFloat()

                    fun project(pt: AirPoint3D): Offset {
                        val cx = pt.x - 0.5f
                        val cy = pt.y - 0.5f
                        val cz = if (is3DMode) pt.z else 0f

                        val rx = cx * cosY - cz * sinY
                        val rz1 = cx * sinY + cz * cosY
                        val ry = cy * cosP - rz1 * sinP
                        val rz2 = cy * sinP + rz1 * cosP

                        val perspective = 1.6f / (1.6f - rz2).coerceAtLeast(0.45f)
                        return Offset(
                            x = (0.5f + rx * perspective) * size.width,
                            y = (0.5f + ry * perspective) * size.height
                        )
                    }

                    strokes.forEach { stroke ->
                        if (stroke.points.isEmpty()) return@forEach
                        val strokeColor = Color(stroke.colorArgb.toULong())
                        val path = Path()
                        stroke.points.forEachIndexed { idx, p ->
                            val projected = project(p)
                            if (idx == 0) path.moveTo(projected.x, projected.y)
                            else path.lineTo(projected.x, projected.y)
                        }
                        // Outer neon glow + crisp core
                        drawPath(
                            path = path,
                            color = strokeColor.copy(alpha = 0.28f),
                            style = Stroke(width = stroke.brushSizeDp * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = stroke.brushSizeDp, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Live index cursor reticle
                    activeFinger?.indexTip?.let { tip ->
                        val cursor = Offset(tip.x * size.width, tip.y * size.height)
                        drawCircle(color = ElectricCyan.copy(alpha = 0.3f), radius = 24f, center = cursor)
                        drawCircle(color = PureWhite, radius = 7f, center = cursor)
                    }
                }

                // Floating Undo / Redo / Clear toolbar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PitchBlack.copy(alpha = 0.78f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onUndo, enabled = strokes.isNotEmpty(), modifier = Modifier.testTag("air_undo_button")) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo stroke", tint = PureWhite)
                    }
                    IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.testTag("air_redo_button")) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo stroke", tint = PureWhite)
                    }
                    IconButton(onClick = onClear, enabled = strokes.isNotEmpty(), modifier = Modifier.testTag("air_clear_button")) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear canvas", tint = CyberPurple)
                    }
                }

                if (orbitMode) {
                    StatusPill(
                        label = "3D ORBIT: Yaw ${orbitYaw.toInt()}°",
                        color = CyberPurple,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }
            }
        }

        // Color Palette, Brush Size & Weapon/Shape Blueprints
        item {
            GlassPanel(accentColor = NeonBlue) {
                Text("Brush Color & Size (${brushSize.toInt()} px)", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AirDrawingPalette.forEach { (name, color) ->
                        val selected = color.value.toLong() == activeColorArgb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) PureWhite else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(color.value.toLong()) }
                                .testTag("color_${name.lowercase().replace(" ", "_")}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = brushSize,
                    onValueChange = onBrushSizeChange,
                    valueRange = 3f..26f
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Quick 3D Weapon & Shape Blueprints", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AirShapeTemplate.entries.filter { it != AirShapeTemplate.FREEHAND }.forEach { tpl ->
                        OutlinedButton(
                            onClick = { onInsertShapePreset(tpl) },
                            modifier = Modifier.testTag("air_template_${tpl.name.lowercase()}")
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(tpl.label)
                        }
                    }
                }
            }
        }

        // Save & Export OBJ/glTF
        item {
            GlassPanel(accentColor = CyberPurple) {
                Text("Save & Export 3D Creation", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artworkTitle,
                    onValueChange = { artworkTitle = it },
                    label = { Text("Creation Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onSaveAndExport(artworkTitle) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_export_drawing_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Save, Export 3D (.OBJ/.glTF) & Add to Library", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedDrawings.isNotEmpty()) {
            item {
                SectionHeader(title = "Saved 3D Creations (${savedDrawings.size})")
            }
            items(savedDrawings, key = { it.id }) { item ->
                GlassPanel(accentColor = ElectricCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text(
                                "${item.strokeCount} strokes • ${item.pointCount} 3D vertices • OBJ + glTF ready",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoolSlate
                            )
                        }
                        IconButton(onClick = { onShareExport(item.exportedObjPath ?: item.exportedGltfPath) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share 3D file", tint = ElectricCyan)
                        }
                        IconButton(onClick = { onDeleteSaved(item.id) }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Delete creation", tint = CoolSlate)
                        }
                    }
                }
            }
        }
    }
}
