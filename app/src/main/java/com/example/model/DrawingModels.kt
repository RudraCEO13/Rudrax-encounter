package com.example.model

import androidx.compose.ui.graphics.Color

data class AirPoint3D(
    val x: Float,
    val y: Float,
    val z: Float
)

data class AirStroke3D(
    val id: Long,
    val points: List<AirPoint3D>,
    val colorArgb: Long,
    val brushSizeDp: Float,
    val is3DMode: Boolean,
    val handUsed: HandSide
)

data class SavedAirDrawing(
    val id: String,
    val title: String,
    val strokeCount: Int,
    val pointCount: Int,
    val strokesJson: String,
    val exportedObjPath: String? = null,
    val exportedGltfPath: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class AirShapeTemplate(val label: String) {
    FREEHAND("Freehand"),
    ENERGY_SWORD("Cyber Sword"),
    PLASMA_BLASTER("Blaster Frame"),
    HOLO_SHIELD("Hex Shield"),
    CROWN_ARTIFACT("3D Crown")
}

val AirDrawingPalette: List<Pair<String, Color>> = listOf(
    "Electric Cyan" to Color(0xFF00F0FF),
    "Neon Blue" to Color(0xFF2563EB),
    "Cyber Purple" to Color(0xFF8B5CF6),
    "Plasma Pink" to Color(0xFFEC4899),
    "Emerald Laser" to Color(0xFF10B981),
    "Solar Gold" to Color(0xFFF59E0B),
    "Pure White" to Color(0xFFF8FAFC)
)
