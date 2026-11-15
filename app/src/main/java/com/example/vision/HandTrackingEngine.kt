package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.DualHandTrackingFrame
import com.example.model.GestureType
import com.example.model.HandSide
import com.example.model.HandTelemetry
import com.example.model.MotionSettingsState
import com.example.model.NormalizedLandmarkPoint
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class HandTrackingEngine(private val context: Context) {

    private val _trackingFrame = MutableStateFlow(DualHandTrackingFrame())
    val trackingFrame: StateFlow<DualHandTrackingFrame> = _trackingFrame.asStateFlow()

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    private var handLandmarker: HandLandmarker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val isProcessing = AtomicBoolean(false)

    private var settings = MotionSettingsState()
    private var lastLeftHand: HandTelemetry? = null
    private var lastRightHand: HandTelemetry? = null
    private var lastHandsDistance: Float? = null

    private var frameCountWindow = 0
    private var lastFpsTimestampMs = SystemClock.uptimeMillis()
    private var measuredFps = 0
    private var lastFrameStartTimeMs = 0L

    fun updateSettings(newSettings: MotionSettingsState) {
        val thresholdChanged = newSettings.minDetectionConfidence != settings.minDetectionConfidence ||
            newSettings.minTrackingConfidence != settings.minTrackingConfidence
        settings = newSettings
        if (thresholdChanged && handLandmarker != null) {
            initializeLandmarker()
        }
    }

    @Synchronized
    fun initializeLandmarker(): Boolean {
        return try {
            handLandmarker?.close()
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_ASSET)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)
                .setMinHandDetectionConfidence(settings.minDetectionConfidence)
                .setMinHandPresenceConfidence(settings.minDetectionConfidence)
                .setMinTrackingConfidence(settings.minTrackingConfidence)
                .setResultListener { result, _ ->
                    onLandmarkerResult(result)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "MediaPipe error: ${error.message}", error)
                    _engineError.value = error.message
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            _engineError.value = null
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize HandLandmarker", t)
            _engineError.value = "HandLandmarker unavailable: ${t.localizedMessage}"
            false
        }
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null
    ) {
        if (handLandmarker == null) {
            initializeLandmarker()
        }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                bindUseCases(provider, lifecycleOwner, previewView)
            } catch (t: Throwable) {
                Log.e(TAG, "Camera initialization failed", t)
                _engineError.value = "Camera error: ${t.localizedMessage}"
                _trackingFrame.value = _trackingFrame.value.copy(
                    isCameraActive = false,
                    trackingQualityHint = "Camera unavailable"
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?
    ) {
        val selector = if (settings.useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(settings.resolutionPreset.width, settings.resolutionPreset.height),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            processImageProxy(imageProxy)
        }

        provider.unbindAll()
        if (previewView != null) {
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
        } else {
            provider.bindToLifecycle(lifecycleOwner, selector, imageAnalysis)
        }

        _trackingFrame.value = _trackingFrame.value.copy(
            isCameraActive = true,
            trackingQualityHint = "Scanning for hands..."
        )
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Throwable) {
        }
        lastLeftHand = null
        lastRightHand = null
        lastHandsDistance = null
        _trackingFrame.value = DualHandTrackingFrame(
            isCameraActive = false,
            trackingQualityHint = "Camera paused"
        )
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val landmarker = handLandmarker
        if (landmarker == null || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        try {
            val rawBitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
            val matrix = Matrix().apply {
                postRotate(rotation)
                if (settings.useFrontCamera) {
                    postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
                }
            }
            val uprightBitmap = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )

            // Quick center brightness check for adaptive low-light advisory
            val lowLight = estimateLowLight(uprightBitmap)
            if (lowLight != _trackingFrame.value.isLowLight) {
                _trackingFrame.value = _trackingFrame.value.copy(isLowLight = lowLight)
            }

            lastFrameStartTimeMs = SystemClock.uptimeMillis()
            val mpImage = BitmapImageBuilder(uprightBitmap).build()
            landmarker.detectAsync(mpImage, lastFrameStartTimeMs)
        } catch (t: Throwable) {
            Log.w(TAG, "Frame analysis skipped: ${t.message}")
            isProcessing.set(false)
        } finally {
            imageProxy.close()
        }
    }

    private fun onLandmarkerResult(result: HandLandmarkerResult) {
        try {
            val now = SystemClock.uptimeMillis()
            val latency = (now - lastFrameStartTimeMs).coerceAtLeast(1L)
            frameCountWindow++
            if (now - lastFpsTimestampMs >= 1000L) {
                measuredFps = frameCountWindow
                frameCountWindow = 0
                lastFpsTimestampMs = now
            }

            var leftHand: HandTelemetry? = null
            var rightHand: HandTelemetry? = null

            val landmarksList = result.landmarks()
            val handednesses = result.handednesses()

            for (i in landmarksList.indices) {
                val category = handednesses.getOrNull(i)?.firstOrNull()
                val label = category?.categoryName().orEmpty()
                val score = category?.score() ?: 0.8f
                val side = if (label.equals("Left", ignoreCase = true)) HandSide.LEFT else HandSide.RIGHT
                val points = landmarksList[i].mapIndexed { idx, lm ->
                    NormalizedLandmarkPoint(
                        index = idx,
                        x = lm.x().coerceIn(0f, 1f),
                        y = lm.y().coerceIn(0f, 1f),
                        z = lm.z()
                    )
                }
                if (points.size == 21) {
                    val prev = if (side == HandSide.LEFT) lastLeftHand else lastRightHand
                    val analyzed = GestureRecognizer.analyzeHand(
                        side = side,
                        rawLandmarks = points,
                        previousHand = prev,
                        confidence = score,
                        smoothingAlpha = settings.smoothingAlpha,
                        invertX = settings.invertXAxis,
                        invertY = settings.invertYAxis
                    )
                    if (side == HandSide.LEFT) leftHand = analyzed else rightHand = analyzed
                }
            }

            lastLeftHand = leftHand
            lastRightHand = rightHand
            val handsDistance = GestureRecognizer.computeHandsDistance(leftHand, rightHand)
            val combinedGesture = GestureRecognizer.detectTwoHandGesture(
                leftHand, rightHand, lastHandsDistance, handsDistance
            )
            lastHandsDistance = handsDistance

            val hint = when {
                leftHand != null && rightHand != null -> "Dual-Hand Lock (42 Landmarks)"
                leftHand != null -> "Left Hand Locked (21 Landmarks)"
                rightHand != null -> "Right Hand Locked (21 Landmarks)"
                _trackingFrame.value.isLowLight -> "Low Light Detected • Improve Illumination"
                else -> "Show hands clearly inside camera frame"
            }

            _trackingFrame.value = DualHandTrackingFrame(
                leftHand = leftHand,
                rightHand = rightHand,
                handsDistance = handsDistance,
                combinedGesture = combinedGesture,
                fps = measuredFps,
                inferenceLatencyMs = latency,
                trackingQualityHint = hint,
                isCameraActive = true,
                isLowLight = _trackingFrame.value.isLowLight,
                timestampMs = System.currentTimeMillis()
            )
        } finally {
            isProcessing.set(false)
        }
    }

    /**
     * Allows calibrated dry-run/test frame injection when testing gestures or running automated verification.
     */
    fun publishCalibratedTestGesture(gesture: GestureType, side: HandSide = HandSide.RIGHT) {
        val baseX = when (gesture) {
            GestureType.MOVE_LEFT -> 0.28f
            GestureType.MOVE_RIGHT -> 0.74f
            else -> 0.52f
        }
        val baseY = when (gesture) {
            GestureType.RAISE_HAND -> 0.25f
            GestureType.LOWER_HAND -> 0.76f
            else -> 0.50f
        }
        val syntheticPoints = (0..20).map { idx ->
            val angle = idx * 0.25f
            val spread = if (gesture == GestureType.CLOSED_FIST) 0.03f else 0.09f
            NormalizedLandmarkPoint(
                index = idx,
                x = (baseX + kotlin.math.cos(angle) * spread * (idx % 5) * 0.25f).coerceIn(0.05f, 0.95f),
                y = (baseY - (idx % 5) * spread * 0.35f).coerceIn(0.05f, 0.95f),
                z = if (gesture == GestureType.PUSH_FORWARD) -0.08f else 0f
            )
        }
        val hand = GestureRecognizer.analyzeHand(
            side = side,
            rawLandmarks = syntheticPoints,
            previousHand = null,
            confidence = 0.94f,
            smoothingAlpha = settings.smoothingAlpha
        ).copy(
            activeGesture = gesture,
            isPinching = gesture == GestureType.PINCH,
            isClosedFist = gesture == GestureType.CLOSED_FIST,
            isOpenPalm = gesture == GestureType.OPEN_PALM,
            isIndexPointing = gesture == GestureType.INDEX_POINT
        )
        _trackingFrame.value = DualHandTrackingFrame(
            leftHand = if (side == HandSide.LEFT) hand else _trackingFrame.value.leftHand,
            rightHand = if (side == HandSide.RIGHT) hand else _trackingFrame.value.rightHand,
            handsDistance = null,
            combinedGesture = gesture,
            fps = 30,
            inferenceLatencyMs = 12L,
            trackingQualityHint = "Test Gesture: ${gesture.displayName}",
            isCameraActive = _trackingFrame.value.isCameraActive,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun estimateLowLight(bitmap: Bitmap): Boolean {
        val stepX = (bitmap.width / 8).coerceAtLeast(1)
        val stepY = (bitmap.height / 8).coerceAtLeast(1)
        var sum = 0L
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                sum += (r * 299 + g * 587 + b * 114) / 1000
                count++
                x += stepX
            }
            y += stepY
        }
        val avgLuma = if (count > 0) sum / count else 128L
        return avgLuma < 42L
    }

    fun release() {
        stopCamera()
        handLandmarker?.close()
        handLandmarker = null
        analysisExecutor.shutdownNow()
    }

    companion object {
        private const val TAG = "HandTrackingEngine"
        private const val MODEL_ASSET = "hand_landmarker.task"
    }
}
