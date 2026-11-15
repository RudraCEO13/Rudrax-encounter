package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ObjectTransformState
import com.example.model.VirtualObject3D
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
fun ObjectLibraryScreen(
    builtInObjects: List<VirtualObject3D>,
    customObjects: List<VirtualObject3D>,
    selectedObjectId: String,
    transformState: ObjectTransformState,
    onSelectObject: (VirtualObject3D) -> Unit,
    onImportGlbUri: (Uri) -> Unit,
    onUpdateTransform: ((ObjectTransformState) -> ObjectTransformState) -> Unit,
    onPlaceInGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allObjects = builtInObjects + customObjects
    val activeObject = allObjects.firstOrNull { it.id == selectedObjectId } ?: builtInObjects.first()

    val glbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportGlbUri(uri)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("object_library_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "3D Virtual Weapon & Object Library",
                subtitle = "Interactive virtual game assets • Support GLB / glTF & Air Creations"
            )
        }

        // Interactive 3D Inspection & Hand-Transform Stage
        item {
            val accent = Color(activeObject.accentHex.toULong())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF04070E))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(20.dp))
            ) {
                InteractiveObject3DCanvas(
                    geometryPreset = activeObject.geometryPreset,
                    accentColor = accent,
                    transform = transformState,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(label = "VIRTUAL GAME ASSET", color = accent)
                    if (transformState.isPlacedInGame) {
                        StatusPill(label = "EQUIPPED IN GAME", color = EmeraldSignal)
                    }
                }
                Text(
                    text = "${activeObject.name} • Yaw ${transformState.rotationYaw.toInt()}° • Scale ${"%.2f".format(transformState.scale)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = PureWhite,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }
        }

        // Hand-Controlled Transform + Place in Game
        item {
            GlassPanel(highlighted = true, accentColor = ElectricCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hand Gesture 3D Manipulation", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("Move palm, rotate wrist, or spread hands to transform", style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                    }
                    Switch(
                        checked = transformState.handControlEnabled,
                        onCheckedChange = { enabled -> onUpdateTransform { it.copy(handControlEnabled = enabled) } }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Yaw Rotation: ${transformState.rotationYaw.toInt()}°", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                Slider(
                    value = transformState.rotationYaw,
                    onValueChange = { v -> onUpdateTransform { it.copy(rotationYaw = v) } },
                    valueRange = -180f..180f
                )
                Text("Scale: ${"%.2f".format(transformState.scale)}x", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                Slider(
                    value = transformState.scale,
                    onValueChange = { v -> onUpdateTransform { it.copy(scale = v) } },
                    valueRange = 0.5f..2.2f
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPlaceInGame,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("place_object_in_game_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Place in Game", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { glbLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.testTag("import_glb_button")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("GLB/glTF")
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Asset Arsenal (${allObjects.size})")
        }

        items(allObjects, key = { it.id }) { obj ->
            val selected = obj.id == activeObject.id
            val accent = Color(obj.accentHex.toULong())
            GlassPanel(
                highlighted = selected,
                accentColor = accent,
                onClick = { onSelectObject(obj) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("object_card_${obj.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = obj.name, tint = accent)
                        Column {
                            Text(obj.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text("${obj.category.label} • ${obj.subtitle}", style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                        }
                    }
                    if (selected) {
                        StatusPill(label = "ACTIVE", color = accent)
                    }
                }
            }
        }
    }
}

private data class Vec3(val x: Float, val y: Float, val z: Float)

@Composable
private fun InteractiveObject3DCanvas(
    geometryPreset: String,
    accentColor: Color,
    transform: ObjectTransformState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val edges: List<Pair<Vec3, Vec3>> = buildPresetMeshEdges(geometryPreset)
        val yaw = Math.toRadians(transform.rotationYaw.toDouble())
        val pitch = Math.toRadians(transform.rotationPitch.toDouble())
        val roll = Math.toRadians(transform.rotationRoll.toDouble())

        val cy = cos(yaw).toFloat(); val sy = sin(yaw).toFloat()
        val cp = cos(pitch).toFloat(); val sp = sin(pitch).toFloat()
        val cr = cos(roll).toFloat(); val sr = sin(roll).toFloat()

        fun project(v: Vec3): Offset {
            // Roll -> Pitch -> Yaw
            val x1 = v.x * cr - v.y * sr
            val y1 = v.x * sr + v.y * cr
            val z1 = v.z

            val y2 = y1 * cp - z1 * sp
            val z2 = y1 * sp + z1 * cp

            val x3 = x1 * cy + z2 * sy
            val z3 = -x1 * sy + z2 * cy

            val scalePx = size.minDimension * 0.32f * transform.scale
            val perspective = 2.6f / (2.6f - z3 * 0.6f).coerceAtLeast(0.6f)
            return Offset(
                x = size.width * (0.5f + transform.positionX * 0.25f) + x3 * scalePx * perspective,
                y = size.height * (0.5f - transform.positionY * 0.25f) - y2 * scalePx * perspective
            )
        }

        edges.forEach { (a, b) ->
            val p1 = project(a)
            val p2 = project(b)
            drawLine(color = accentColor.copy(alpha = 0.28f), start = p1, end = p2, strokeWidth = 10f)
            drawLine(color = accentColor, start = p1, end = p2, strokeWidth = 3.5f)
            drawCircle(color = PureWhite, radius = 3.5f, center = p1)
        }
    }
}

private fun buildPresetMeshEdges(preset: String): List<Pair<Vec3, Vec3>> {
    return when (preset) {
        "ak47", "futuristic" -> listOf(
            // Stock + Receiver + Barrel + Magazine + Sight
            Vec3(-1.1f, -0.25f, 0f) to Vec3(-0.6f, 0.1f, 0f),
            Vec3(-0.6f, 0.1f, 0f) to Vec3(0.45f, 0.1f, 0f),
            Vec3(0.45f, 0.1f, 0f) to Vec3(0.45f, -0.15f, 0f),
            Vec3(0.45f, -0.15f, 0f) to Vec3(-0.6f, -0.15f, 0f),
            Vec3(-0.6f, -0.15f, 0f) to Vec3(-1.1f, -0.25f, 0f),
            Vec3(0.45f, 0.05f, 0f) to Vec3(1.25f, 0.05f, 0f),
            Vec3(0.45f, 0.14f, 0f) to Vec3(1.05f, 0.14f, 0f),
            Vec3(0.05f, -0.15f, 0f) to Vec3(0.22f, -0.68f, 0f),
            Vec3(0.22f, -0.68f, 0f) to Vec3(0.36f, -0.62f, 0f),
            Vec3(0.36f, -0.62f, 0f) to Vec3(0.22f, -0.15f, 0f),
            Vec3(-0.35f, -0.15f, 0f) to Vec3(-0.48f, -0.55f, 0f),
            Vec3(-0.15f, 0.1f, 0.08f) to Vec3(0.25f, 0.24f, 0.08f),
            Vec3(-0.15f, 0.1f, -0.08f) to Vec3(0.25f, 0.24f, -0.08f)
        )
        "pistol" -> listOf(
            Vec3(-0.55f, 0.2f, 0f) to Vec3(0.65f, 0.2f, 0f),
            Vec3(0.65f, 0.2f, 0f) to Vec3(0.65f, -0.05f, 0f),
            Vec3(0.65f, -0.05f, 0f) to Vec3(-0.3f, -0.05f, 0f),
            Vec3(-0.3f, -0.05f, 0f) to Vec3(-0.55f, -0.68f, 0f),
            Vec3(-0.55f, -0.68f, 0f) to Vec3(-0.28f, -0.7f, 0f),
            Vec3(-0.28f, -0.7f, 0f) to Vec3(-0.08f, -0.05f, 0f),
            Vec3(-0.05f, -0.05f, 0.08f) to Vec3(0.55f, 0.2f, 0.08f)
        )
        "sword" -> listOf(
            Vec3(0f, -1.0f, 0f) to Vec3(0f, -0.5f, 0f),
            Vec3(-0.35f, -0.5f, 0.08f) to Vec3(0.35f, -0.5f, -0.08f),
            Vec3(-0.08f, -0.5f, 0f) to Vec3(-0.05f, 0.85f, 0f),
            Vec3(-0.05f, 0.85f, 0f) to Vec3(0f, 1.1f, 0f),
            Vec3(0f, 1.1f, 0f) to Vec3(0.09f, 0.8f, 0f),
            Vec3(0.09f, 0.8f, 0f) to Vec3(0.08f, -0.5f, 0f)
        )
        "shield" -> {
            val pts = (0..5).map { i ->
                val a = Math.toRadians(i * 60.0)
                Vec3(0.85f * cos(a).toFloat(), 0.95f * sin(a).toFloat(), 0.12f)
            }
            pts.indices.map { i -> pts[i] to pts[(i + 1) % pts.size] } +
                pts.map { p -> p to Vec3(0f, 0f, -0.25f) }
        }
        else -> {
            // Octahedron / Orb / Custom 3D Frame
            val top = Vec3(0f, 0.9f, 0f)
            val bot = Vec3(0f, -0.9f, 0f)
            val ring = listOf(
                Vec3(0.8f, 0f, 0f),
                Vec3(0f, 0f, 0.8f),
                Vec3(-0.8f, 0f, 0f),
                Vec3(0f, 0f, -0.8f)
            )
            ring.indices.flatMap { i ->
                val p = ring[i]
                val next = ring[(i + 1) % ring.size]
                listOf(top to p, bot to p, p to next)
            }
        }
    }
}
