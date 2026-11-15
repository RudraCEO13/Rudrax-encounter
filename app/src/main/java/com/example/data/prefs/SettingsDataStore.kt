package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CameraResolutionPreset
import com.example.model.MotionSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.motionDataStore by preferencesDataStore(name = "motionx_settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val RESOLUTION = stringPreferencesKey("resolution_preset")
        val USE_FRONT_CAMERA = booleanPreferencesKey("use_front_camera")
        val SMOOTHING_ALPHA = floatPreferencesKey("smoothing_alpha")
        val MIN_DETECTION_CONF = floatPreferencesKey("min_detection_conf")
        val MIN_TRACKING_CONF = floatPreferencesKey("min_tracking_conf")
        val HTTP_PORT = intPreferencesKey("http_port")
        val WS_PORT = intPreferencesKey("ws_port")
        val ENABLE_WEBRTC = booleanPreferencesKey("enable_webrtc")
        val ENABLE_BLUETOOTH = booleanPreferencesKey("enable_bluetooth")
        val INVERT_X = booleanPreferencesKey("invert_x")
        val INVERT_Y = booleanPreferencesKey("invert_y")
    }

    val settingsFlow: Flow<MotionSettingsState> = context.motionDataStore.data.map { prefs ->
        val presetName = prefs[Keys.RESOLUTION] ?: CameraResolutionPreset.BALANCED.name
        val preset = runCatching { CameraResolutionPreset.valueOf(presetName) }
            .getOrDefault(CameraResolutionPreset.BALANCED)
        MotionSettingsState(
            resolutionPreset = preset,
            useFrontCamera = prefs[Keys.USE_FRONT_CAMERA] ?: true,
            smoothingAlpha = prefs[Keys.SMOOTHING_ALPHA] ?: 0.45f,
            minDetectionConfidence = prefs[Keys.MIN_DETECTION_CONF] ?: 0.55f,
            minTrackingConfidence = prefs[Keys.MIN_TRACKING_CONF] ?: 0.55f,
            httpPort = prefs[Keys.HTTP_PORT] ?: 8088,
            wsPort = prefs[Keys.WS_PORT] ?: 8765,
            enableWebRtcDataChannel = prefs[Keys.ENABLE_WEBRTC] ?: true,
            enableBluetoothDiscovery = prefs[Keys.ENABLE_BLUETOOTH] ?: true,
            invertXAxis = prefs[Keys.INVERT_X] ?: false,
            invertYAxis = prefs[Keys.INVERT_Y] ?: false
        )
    }

    suspend fun update(transform: (MotionSettingsState) -> MotionSettingsState) {
        context.motionDataStore.edit { prefs ->
            val current = MotionSettingsState(
                resolutionPreset = runCatching {
                    CameraResolutionPreset.valueOf(
                        prefs[Keys.RESOLUTION] ?: CameraResolutionPreset.BALANCED.name
                    )
                }.getOrDefault(CameraResolutionPreset.BALANCED),
                useFrontCamera = prefs[Keys.USE_FRONT_CAMERA] ?: true,
                smoothingAlpha = prefs[Keys.SMOOTHING_ALPHA] ?: 0.45f,
                minDetectionConfidence = prefs[Keys.MIN_DETECTION_CONF] ?: 0.55f,
                minTrackingConfidence = prefs[Keys.MIN_TRACKING_CONF] ?: 0.55f,
                httpPort = prefs[Keys.HTTP_PORT] ?: 8088,
                wsPort = prefs[Keys.WS_PORT] ?: 8765,
                enableWebRtcDataChannel = prefs[Keys.ENABLE_WEBRTC] ?: true,
                enableBluetoothDiscovery = prefs[Keys.ENABLE_BLUETOOTH] ?: true,
                invertXAxis = prefs[Keys.INVERT_X] ?: false,
                invertYAxis = prefs[Keys.INVERT_Y] ?: false
            )
            val updated = transform(current)
            prefs[Keys.RESOLUTION] = updated.resolutionPreset.name
            prefs[Keys.USE_FRONT_CAMERA] = updated.useFrontCamera
            prefs[Keys.SMOOTHING_ALPHA] = updated.smoothingAlpha
            prefs[Keys.MIN_DETECTION_CONF] = updated.minDetectionConfidence
            prefs[Keys.MIN_TRACKING_CONF] = updated.minTrackingConfidence
            prefs[Keys.HTTP_PORT] = updated.httpPort
            prefs[Keys.WS_PORT] = updated.wsPort
            prefs[Keys.ENABLE_WEBRTC] = updated.enableWebRtcDataChannel
            prefs[Keys.ENABLE_BLUETOOTH] = updated.enableBluetoothDiscovery
            prefs[Keys.INVERT_X] = updated.invertXAxis
            prefs[Keys.INVERT_Y] = updated.invertYAxis
        }
    }
}
