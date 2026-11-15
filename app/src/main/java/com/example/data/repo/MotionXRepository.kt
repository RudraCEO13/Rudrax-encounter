package com.example.data.repo

import com.example.data.db.CustomGameEntity
import com.example.data.db.CustomObject3DEntity
import com.example.data.db.GestureMappingEntity
import com.example.data.db.MotionXDatabase
import com.example.data.db.PairedDeviceEntity
import com.example.data.db.SavedDrawingEntity
import com.example.data.prefs.SettingsDataStore
import com.example.model.CustomGameProject
import com.example.model.DiscoveredDevice
import com.example.model.DiscoverySource
import com.example.model.GameActionTarget
import com.example.model.GameAssetFile
import com.example.model.GestureMappingRule
import com.example.model.GestureType
import com.example.model.HandSide
import com.example.model.MotionSettingsState
import com.example.model.SavedAirDrawing
import com.example.model.TargetDeviceType
import com.example.model.VirtualObject3D
import com.example.model.VirtualObjectCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class MotionXRepository(
    private val db: MotionXDatabase,
    private val settingsStore: SettingsDataStore
) {
    val settingsFlow: Flow<MotionSettingsState> = settingsStore.settingsFlow

    val gamesFlow: Flow<List<CustomGameProject>> = db.customGameDao().observeAll().map { entities ->
        entities.map { entity ->
            CustomGameProject(
                id = entity.id,
                title = entity.title,
                entryPoint = entity.entryPoint,
                rootDirectory = entity.rootDirectory,
                files = parseFilesJson(entity.filesJson),
                sourceType = entity.sourceType,
                targetOrientation = entity.targetOrientation,
                sensitivityMultiplier = entity.sensitivityMultiplier,
                isBuiltInTemplate = entity.isBuiltInTemplate,
                createdAtMs = entity.createdAtMs
            )
        }
    }

    val gestureMappingsFlow: Flow<List<GestureMappingRule>> =
        db.gestureMappingDao().observeAll().map { entities ->
            entities.map { e ->
                GestureMappingRule(
                    id = e.id,
                    handSide = runCatching { HandSide.valueOf(e.handSide) }.getOrDefault(HandSide.RIGHT),
                    gesture = runCatching { GestureType.valueOf(e.gestureName) }.getOrDefault(GestureType.NONE),
                    actionTarget = runCatching { GameActionTarget.valueOf(e.actionTargetName) }
                        .getOrDefault(GameActionTarget.SELECT_INTERACT),
                    customSdkEvent = e.customSdkEvent,
                    enabled = e.enabled,
                    sensitivity = e.sensitivity,
                    deadzone = e.deadzone
                )
            }
        }

    val savedDrawingsFlow: Flow<List<SavedAirDrawing>> =
        db.savedDrawingDao().observeAll().map { entities ->
            entities.map { e ->
                SavedAirDrawing(
                    id = e.id,
                    title = e.title,
                    strokeCount = e.strokeCount,
                    pointCount = e.pointCount,
                    strokesJson = e.strokesJson,
                    exportedObjPath = e.exportedObjPath,
                    exportedGltfPath = e.exportedGltfPath,
                    createdAtMs = e.createdAtMs
                )
            }
        }

    val pairedDevicesFlow: Flow<List<DiscoveredDevice>> =
        db.pairedDeviceDao().observeAll().map { entities ->
            entities.map { e ->
                DiscoveredDevice(
                    id = e.id,
                    name = e.name,
                    address = e.address,
                    port = e.port,
                    type = runCatching { TargetDeviceType.valueOf(e.typeName) }
                        .getOrDefault(TargetDeviceType.SMART_TV),
                    source = runCatching { DiscoverySource.valueOf(e.sourceName) }
                        .getOrDefault(DiscoverySource.WIFI_NSD),
                    isPaired = true
                )
            }
        }

    val customObjectsFlow: Flow<List<VirtualObject3D>> =
        db.customObject3DDao().observeAll().map { entities ->
            entities.map { e ->
                VirtualObject3D(
                    id = e.id,
                    name = e.name,
                    subtitle = e.subtitle,
                    category = runCatching { VirtualObjectCategory.valueOf(e.categoryName) }
                        .getOrDefault(VirtualObjectCategory.CUSTOM_GLB),
                    geometryPreset = e.geometryPreset,
                    accentHex = e.accentHex,
                    customModelPath = e.customModelPath,
                    isVirtualGameAsset = true
                )
            }
        }

    suspend fun ensureDefaultGestureMappings() {
        if (db.gestureMappingDao().count() > 0) return
        val defaults = defaultGestureMappings().map { r ->
            GestureMappingEntity(
                id = r.id,
                handSide = r.handSide.name,
                gestureName = r.gesture.name,
                actionTargetName = r.actionTarget.name,
                customSdkEvent = r.customSdkEvent,
                enabled = r.enabled,
                sensitivity = r.sensitivity,
                deadzone = r.deadzone
            )
        }
        db.gestureMappingDao().upsertAll(defaults)
    }

    suspend fun saveCustomGame(game: CustomGameProject) {
        val arr = JSONArray()
        game.files.forEach { f ->
            arr.put(JSONObject().apply {
                put("path", f.relativePath)
                put("ext", f.extension)
                put("size", f.sizeBytes)
            })
        }
        db.customGameDao().upsert(
            CustomGameEntity(
                id = game.id,
                title = game.title,
                entryPoint = game.entryPoint,
                rootDirectory = game.rootDirectory,
                filesJson = arr.toString(),
                sourceType = game.sourceType,
                targetOrientation = game.targetOrientation,
                sensitivityMultiplier = game.sensitivityMultiplier,
                isBuiltInTemplate = game.isBuiltInTemplate,
                createdAtMs = game.createdAtMs
            )
        )
    }

    suspend fun deleteCustomGame(id: String) = db.customGameDao().deleteById(id)

    suspend fun saveGestureMapping(rule: GestureMappingRule) {
        db.gestureMappingDao().upsert(
            GestureMappingEntity(
                id = rule.id,
                handSide = rule.handSide.name,
                gestureName = rule.gesture.name,
                actionTargetName = rule.actionTarget.name,
                customSdkEvent = rule.customSdkEvent,
                enabled = rule.enabled,
                sensitivity = rule.sensitivity,
                deadzone = rule.deadzone
            )
        )
    }

    suspend fun resetDefaultGestureMappings() {
        val defaults = defaultGestureMappings().map { r ->
            GestureMappingEntity(
                id = r.id,
                handSide = r.handSide.name,
                gestureName = r.gesture.name,
                actionTargetName = r.actionTarget.name,
                customSdkEvent = r.customSdkEvent,
                enabled = r.enabled,
                sensitivity = r.sensitivity,
                deadzone = r.deadzone
            )
        }
        db.gestureMappingDao().upsertAll(defaults)
    }

    suspend fun saveDrawing(drawing: SavedAirDrawing) {
        db.savedDrawingDao().upsert(
            SavedDrawingEntity(
                id = drawing.id,
                title = drawing.title,
                strokeCount = drawing.strokeCount,
                pointCount = drawing.pointCount,
                strokesJson = drawing.strokesJson,
                exportedObjPath = drawing.exportedObjPath,
                exportedGltfPath = drawing.exportedGltfPath,
                createdAtMs = drawing.createdAtMs
            )
        )
    }

    suspend fun deleteDrawing(id: String) = db.savedDrawingDao().deleteById(id)

    suspend fun savePairedDevice(device: DiscoveredDevice) {
        db.pairedDeviceDao().upsert(
            PairedDeviceEntity(
                id = device.id,
                name = device.name,
                address = device.address,
                port = device.port,
                typeName = device.type.name,
                sourceName = device.source.name,
                lastConnectedMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun removePairedDevice(id: String) = db.pairedDeviceDao().deleteById(id)

    suspend fun saveCustomObject3D(obj: VirtualObject3D) {
        db.customObject3DDao().upsert(
            CustomObject3DEntity(
                id = obj.id,
                name = obj.name,
                subtitle = obj.subtitle,
                categoryName = obj.category.name,
                geometryPreset = obj.geometryPreset,
                accentHex = obj.accentHex,
                customModelPath = obj.customModelPath,
                createdAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSettings(transform: (MotionSettingsState) -> MotionSettingsState) {
        settingsStore.update(transform)
    }

    private fun parseFilesJson(json: String): List<GameAssetFile> {
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                GameAssetFile(
                    relativePath = obj.optString("path"),
                    extension = obj.optString("ext"),
                    sizeBytes = obj.optLong("size")
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        fun defaultGestureMappings(): List<GestureMappingRule> = listOf(
            GestureMappingRule("r_move_right", HandSide.RIGHT, GestureType.MOVE_RIGHT, GameActionTarget.MOVE_RIGHT, "move_right"),
            GestureMappingRule("r_move_left", HandSide.RIGHT, GestureType.MOVE_LEFT, GameActionTarget.MOVE_LEFT, "move_left"),
            GestureMappingRule("r_raise", HandSide.RIGHT, GestureType.RAISE_HAND, GameActionTarget.JUMP, "jump"),
            GestureMappingRule("r_lower", HandSide.RIGHT, GestureType.LOWER_HAND, GameActionTarget.CROUCH, "crouch"),
            GestureMappingRule("r_fist", HandSide.RIGHT, GestureType.CLOSED_FIST, GameActionTarget.GRAB_ACTIVATE, "grab_activate"),
            GestureMappingRule("r_palm", HandSide.RIGHT, GestureType.OPEN_PALM, GameActionTarget.RELEASE, "release"),
            GestureMappingRule("r_pinch", HandSide.RIGHT, GestureType.PINCH, GameActionTarget.SELECT_INTERACT, "select_fire"),
            GestureMappingRule("r_point", HandSide.RIGHT, GestureType.INDEX_POINT, GameActionTarget.AIR_DRAW_ENGAGE, "air_draw"),
            GestureMappingRule("r_rot_cw", HandSide.RIGHT, GestureType.ROTATE_WRIST_CW, GameActionTarget.ROTATE_OBJECT, "rotate_cw"),
            GestureMappingRule("l_push", HandSide.LEFT, GestureType.PUSH_FORWARD, GameActionTarget.PUSH_ACTION, "push_action"),
            GestureMappingRule("l_pull", HandSide.LEFT, GestureType.PULL_BACKWARD, GameActionTarget.PULL_ACTION, "pull_action"),
            GestureMappingRule("l_fist", HandSide.LEFT, GestureType.CLOSED_FIST, GameActionTarget.GRAB_ACTIVATE, "left_shield_grab"),
            GestureMappingRule("l_palm", HandSide.LEFT, GestureType.OPEN_PALM, GameActionTarget.RELEASE, "left_release"),
            GestureMappingRule("l_pinch", HandSide.LEFT, GestureType.PINCH, GameActionTarget.EQUIP_OBJECT, "equip_weapon"),
            GestureMappingRule("l_point", HandSide.LEFT, GestureType.INDEX_POINT, GameActionTarget.AIR_DRAW_ENGAGE, "left_air_draw"),
            GestureMappingRule("l_rot_ccw", HandSide.LEFT, GestureType.ROTATE_WRIST_CCW, GameActionTarget.ROTATE_OBJECT, "rotate_ccw")
        )
    }
}
