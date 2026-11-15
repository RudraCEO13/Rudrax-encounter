package com.example.model

enum class GameActionTarget(val label: String, val keyCode: String) {
    MOVE_LEFT("Move Left", "ArrowLeft"),
    MOVE_RIGHT("Move Right", "ArrowRight"),
    JUMP("Jump / Up", "ArrowUp"),
    CROUCH("Crouch / Down", "ArrowDown"),
    GRAB_ACTIVATE("Grab / Activate", "KeyE"),
    RELEASE("Release", "KeyR"),
    PUSH_ACTION("Push / Strike", "Space"),
    PULL_ACTION("Pull / Reload", "KeyQ"),
    ROTATE_OBJECT("Rotate Object", "KeyT"),
    SELECT_INTERACT("Select / Fire", "Enter"),
    AIR_DRAW_ENGAGE("Air Draw Stroke", "Draw"),
    EQUIP_OBJECT("Equip 3D Asset", "Equip")
}

data class GestureMappingRule(
    val id: String,
    val handSide: HandSide,
    val gesture: GestureType,
    val actionTarget: GameActionTarget,
    val customSdkEvent: String,
    val enabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val deadzone: Float = 0.08f
)

data class MotionSettingsState(
    val resolutionPreset: CameraResolutionPreset = CameraResolutionPreset.BALANCED,
    val useFrontCamera: Boolean = true,
    val smoothingAlpha: Float = 0.45f,
    val minDetectionConfidence: Float = 0.55f,
    val minTrackingConfidence: Float = 0.55f,
    val httpPort: Int = 8088,
    val wsPort: Int = 8765,
    val enableWebRtcDataChannel: Boolean = true,
    val enableBluetoothDiscovery: Boolean = true,
    val invertXAxis: Boolean = false,
    val invertYAxis: Boolean = false
)
