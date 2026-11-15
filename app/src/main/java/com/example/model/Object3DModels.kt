package com.example.model

enum class VirtualObjectCategory(val label: String) {
    RIFLE("Assault / Tactical"),
    SIDEARM("Sidearms"),
    MELEE("Melee Blades"),
    FUTURISTIC("Futuristic / Sci-Fi"),
    INTERACTIVE("Interactive Objects"),
    CUSTOM_GLB("Uploaded GLB / glTF"),
    AIR_DRAWN("Air-Drawn Creations")
}

data class VirtualObject3D(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: VirtualObjectCategory,
    val geometryPreset: String,
    val accentHex: Long,
    val customModelPath: String? = null,
    val isVirtualGameAsset: Boolean = true
)

data class ObjectTransformState(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val positionZ: Float = 0f,
    val rotationPitch: Float = 12f,
    val rotationYaw: Float = -28f,
    val rotationRoll: Float = 0f,
    val scale: Float = 1.0f,
    val isPlacedInGame: Boolean = false,
    val handControlEnabled: Boolean = true
)
