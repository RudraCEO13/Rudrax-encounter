package com.example.model

data class GameAssetFile(
    val relativePath: String,
    val extension: String,
    val sizeBytes: Long
)

data class CustomGameProject(
    val id: String,
    val title: String,
    val entryPoint: String,
    val rootDirectory: String,
    val files: List<GameAssetFile>,
    val sourceType: String,
    val supportsMotionSdk: Boolean = true,
    val targetOrientation: String = "Landscape",
    val sensitivityMultiplier: Float = 1.0f,
    val isBuiltInTemplate: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class GameRuntimeState(val label: String) {
    STOPPED("Idle"),
    LOADING("Mounting Runtime"),
    RUNNING("Live Runtime Active"),
    PAUSED("Paused")
}

data class SdkEventLog(
    val id: Long,
    val channel: String,
    val payloadPreview: String,
    val timestampMs: Long = System.currentTimeMillis()
)
