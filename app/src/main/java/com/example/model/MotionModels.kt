package com.example.model

enum class HandSide {
    LEFT,
    RIGHT
}

enum class GestureType(val displayName: String) {
    NONE("Neutral"),
    OPEN_PALM("Open Palm"),
    CLOSED_FIST("Closed Fist"),
    PINCH("Pinch"),
    INDEX_POINT("Index Point"),
    VICTORY("Peace / Dual"),
    THUMBS_UP("Thumbs Up"),
    MOVE_LEFT("Move Left"),
    MOVE_RIGHT("Move Right"),
    RAISE_HAND("Raise Hand"),
    LOWER_HAND("Lower Hand"),
    PUSH_FORWARD("Push Forward"),
    PULL_BACKWARD("Pull Backward"),
    ROTATE_WRIST_CW("Rotate Wrist CW"),
    ROTATE_WRIST_CCW("Rotate Wrist CCW"),
    TWO_HAND_SPREAD("Two-Hand Spread"),
    TWO_HAND_CLAP("Two-Hand Close")
}

data class NormalizedLandmarkPoint(
    val index: Int,
    val x: Float,
    val y: Float,
    val z: Float
)

data class FingerBends(
    val thumb: Float = 0f,
    val index: Float = 0f,
    val middle: Float = 0f,
    val ring: Float = 0f,
    val pinky: Float = 0f
)

data class HandTelemetry(
    val side: HandSide,
    val landmarks: List<NormalizedLandmarkPoint>,
    val wrist: NormalizedLandmarkPoint,
    val indexTip: NormalizedLandmarkPoint,
    val thumbTip: NormalizedLandmarkPoint,
    val palmCenter: NormalizedLandmarkPoint,
    val palmRollDegrees: Float,
    val velocityX: Float,
    val velocityY: Float,
    val velocityZ: Float,
    val fingerBends: FingerBends,
    val pinchDistance: Float,
    val isOpenPalm: Boolean,
    val isClosedFist: Boolean,
    val isPinching: Boolean,
    val isIndexPointing: Boolean,
    val activeGesture: GestureType,
    val confidence: Float
)

data class DualHandTrackingFrame(
    val leftHand: HandTelemetry? = null,
    val rightHand: HandTelemetry? = null,
    val handsDistance: Float? = null,
    val combinedGesture: GestureType = GestureType.NONE,
    val fps: Int = 0,
    val inferenceLatencyMs: Long = 0L,
    val trackingQualityHint: String = "Ready",
    val isCameraActive: Boolean = false,
    val isLowLight: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val totalLandmarksDetected: Int
        get() = (leftHand?.landmarks?.size ?: 0) + (rightHand?.landmarks?.size ?: 0)
}

enum class CameraResolutionPreset(
    val label: String,
    val width: Int,
    val height: Int
) {
    ECO("Eco 320×240 (Low-End Opt)", 320, 240),
    BALANCED("Balanced 640×480", 640, 480),
    HIGH("Precision 960×720", 960, 720)
}
