package com.example.vision

import com.example.model.FingerBends
import com.example.model.GestureType
import com.example.model.HandSide
import com.example.model.HandTelemetry
import com.example.model.NormalizedLandmarkPoint
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Pure geometric hand landmark analyzer & gesture classifier operating on 21 MediaPipe landmarks.
 * Designed for low-latency, deterministic processing and unit testability.
 */
object GestureRecognizer {

    val HAND_CONNECTIONS: List<Pair<Int, Int>> = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,         // Thumb
        0 to 5, 5 to 6, 6 to 7, 7 to 8,         // Index
        0 to 9, 9 to 10, 10 to 11, 11 to 12,    // Middle
        0 to 13, 13 to 14, 14 to 15, 15 to 16,  // Ring
        0 to 17, 17 to 18, 18 to 19, 19 to 20,  // Pinky
        5 to 9, 9 to 13, 13 to 17               // Palm arch
    )

    fun analyzeHand(
        side: HandSide,
        rawLandmarks: List<NormalizedLandmarkPoint>,
        previousHand: HandTelemetry?,
        confidence: Float,
        smoothingAlpha: Float,
        invertX: Boolean = false,
        invertY: Boolean = false,
        deadzone: Float = 0.035f
    ): HandTelemetry {
        val smoothed = if (previousHand != null && previousHand.landmarks.size == rawLandmarks.size) {
            rawLandmarks.mapIndexed { idx, pt ->
                val prev = previousHand.landmarks[idx]
                val a = smoothingAlpha.coerceIn(0.1f, 0.95f)
                NormalizedLandmarkPoint(
                    index = pt.index,
                    x = prev.x + a * (pt.x - prev.x),
                    y = prev.y + a * (pt.y - prev.y),
                    z = prev.z + a * (pt.z - prev.z)
                )
            }
        } else {
            rawLandmarks
        }

        val wrist = smoothed[0]
        val thumbTip = smoothed[4]
        val indexTip = smoothed[8]
        val palmNodes = listOf(smoothed[0], smoothed[5], smoothed[9], smoothed[13], smoothed[17])
        val palmCenter = NormalizedLandmarkPoint(
            index = -1,
            x = palmNodes.map { it.x }.average().toFloat(),
            y = palmNodes.map { it.y }.average().toFloat(),
            z = palmNodes.map { it.z }.average().toFloat()
        )

        // Palm roll angle relative to vertical up (-Y)
        val dxRoll = smoothed[9].x - wrist.x
        val dyRoll = smoothed[9].y - wrist.y
        val palmRollDegrees = Math.toDegrees(atan2(dxRoll.toDouble(), (-dyRoll).toDouble())).toFloat()

        // Finger curl [0f = fully extended, 1f = fully curled]
        val thumbBend = fingerCurl(smoothed[1], smoothed[2], smoothed[3], smoothed[4])
        val indexBend = fingerCurl(smoothed[5], smoothed[6], smoothed[7], smoothed[8])
        val middleBend = fingerCurl(smoothed[9], smoothed[10], smoothed[11], smoothed[12])
        val ringBend = fingerCurl(smoothed[13], smoothed[14], smoothed[15], smoothed[16])
        val pinkyBend = fingerCurl(smoothed[17], smoothed[18], smoothed[19], smoothed[20])
        val bends = FingerBends(thumbBend, indexBend, middleBend, ringBend, pinkyBend)

        // Normalize pinch distance by palm size (wrist to middle MCP)
        val palmScale = distance3D(wrist, smoothed[9]).coerceAtLeast(0.05f)
        val pinchDistance = distance3D(thumbTip, indexTip) / palmScale

        val isPinching = pinchDistance < 0.42f && middleBend < 0.72f
        val isOpenPalm = indexBend < 0.30f && middleBend < 0.30f && ringBend < 0.32f && pinkyBend < 0.35f && !isPinching
        val isClosedFist = indexBend > 0.62f && middleBend > 0.62f && ringBend > 0.62f && pinkyBend > 0.60f
        val isIndexPointing = indexBend < 0.30f && middleBend > 0.55f && ringBend > 0.55f && pinkyBend > 0.55f
        val isVictory = indexBend < 0.32f && middleBend < 0.32f && ringBend > 0.55f && pinkyBend > 0.55f
        val isThumbsUp = thumbBend < 0.25f && indexBend > 0.58f && middleBend > 0.58f && ringBend > 0.58f

        var vx = if (previousHand != null) palmCenter.x - previousHand.palmCenter.x else 0f
        var vy = if (previousHand != null) palmCenter.y - previousHand.palmCenter.y else 0f
        val vz = if (previousHand != null) palmCenter.z - previousHand.palmCenter.z else 0f
        if (invertX) vx = -vx
        if (invertY) vy = -vy

        val rollDelta = if (previousHand != null) palmRollDegrees - previousHand.palmRollDegrees else 0f

        val activeGesture = when {
            isPinching -> GestureType.PINCH
            isClosedFist -> GestureType.CLOSED_FIST
            isIndexPointing -> GestureType.INDEX_POINT
            isVictory -> GestureType.VICTORY
            isThumbsUp -> GestureType.THUMBS_UP
            rollDelta > 9f || palmRollDegrees > 38f -> GestureType.ROTATE_WRIST_CW
            rollDelta < -9f || palmRollDegrees < -38f -> GestureType.ROTATE_WRIST_CCW
            vz < -0.025f -> GestureType.PUSH_FORWARD
            vz > 0.025f -> GestureType.PULL_BACKWARD
            abs(vx) > deadzone && abs(vx) >= abs(vy) -> if (vx > 0f) GestureType.MOVE_RIGHT else GestureType.MOVE_LEFT
            abs(vy) > deadzone -> if (vy < 0f) GestureType.RAISE_HAND else GestureType.LOWER_HAND
            isOpenPalm -> GestureType.OPEN_PALM
            else -> GestureType.NONE
        }

        return HandTelemetry(
            side = side,
            landmarks = smoothed,
            wrist = wrist,
            indexTip = indexTip,
            thumbTip = thumbTip,
            palmCenter = palmCenter,
            palmRollDegrees = palmRollDegrees,
            velocityX = vx,
            velocityY = vy,
            velocityZ = vz,
            fingerBends = bends,
            pinchDistance = pinchDistance,
            isOpenPalm = isOpenPalm,
            isClosedFist = isClosedFist,
            isPinching = isPinching,
            isIndexPointing = isIndexPointing,
            activeGesture = activeGesture,
            confidence = confidence
        )
    }

    fun computeHandsDistance(left: HandTelemetry?, right: HandTelemetry?): Float? {
        if (left == null || right == null) return null
        return hypot(
            (left.palmCenter.x - right.palmCenter.x).toDouble(),
            (left.palmCenter.y - right.palmCenter.y).toDouble()
        ).toFloat()
    }

    fun detectTwoHandGesture(
        left: HandTelemetry?,
        right: HandTelemetry?,
        previousDistance: Float?,
        currentDistance: Float?
    ): GestureType {
        if (left == null || right == null || currentDistance == null) {
            return right?.activeGesture ?: left?.activeGesture ?: GestureType.NONE
        }
        if (previousDistance != null) {
            val delta = currentDistance - previousDistance
            if (delta > 0.055f) return GestureType.TWO_HAND_SPREAD
            if (delta < -0.055f || currentDistance < 0.14f) return GestureType.TWO_HAND_CLAP
        }
        if (left.activeGesture == GestureType.PUSH_FORWARD && right.activeGesture == GestureType.PUSH_FORWARD) {
            return GestureType.PUSH_FORWARD
        }
        return if (right.activeGesture != GestureType.NONE) right.activeGesture else left.activeGesture
    }

    private fun fingerCurl(
        mcp: NormalizedLandmarkPoint,
        pip: NormalizedLandmarkPoint,
        dip: NormalizedLandmarkPoint,
        tip: NormalizedLandmarkPoint
    ): Float {
        val v1x = pip.x - mcp.x
        val v1y = pip.y - mcp.y
        val v1z = pip.z - mcp.z
        val v2x = tip.x - dip.x
        val v2y = tip.y - dip.y
        val v2z = tip.z - dip.z

        val mag1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z).coerceAtLeast(1e-5f)
        val mag2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z).coerceAtLeast(1e-5f)
        val cosTheta = ((v1x * v2x + v1y * v2y + v1z * v2z) / (mag1 * mag2)).coerceIn(-1f, 1f)
        val angleRad = acos(cosTheta)
        return (angleRad / Math.PI.toFloat()).coerceIn(0f, 1f)
    }

    private fun distance3D(a: NormalizedLandmarkPoint, b: NormalizedLandmarkPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
