package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_games")
data class CustomGameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val entryPoint: String,
    val rootDirectory: String,
    val filesJson: String,
    val sourceType: String,
    val targetOrientation: String,
    val sensitivityMultiplier: Float,
    val isBuiltInTemplate: Boolean,
    val createdAtMs: Long
)

@Entity(tableName = "gesture_mappings")
data class GestureMappingEntity(
    @PrimaryKey val id: String,
    val handSide: String,
    val gestureName: String,
    val actionTargetName: String,
    val customSdkEvent: String,
    val enabled: Boolean,
    val sensitivity: Float,
    val deadzone: Float
)

@Entity(tableName = "saved_drawings")
data class SavedDrawingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val strokeCount: Int,
    val pointCount: Int,
    val strokesJson: String,
    val exportedObjPath: String?,
    val exportedGltfPath: String?,
    val createdAtMs: Long
)

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val typeName: String,
    val sourceName: String,
    val lastConnectedMs: Long
)

@Entity(tableName = "custom_objects_3d")
data class CustomObject3DEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val categoryName: String,
    val geometryPreset: String,
    val accentHex: Long,
    val customModelPath: String?,
    val createdAtMs: Long
)
