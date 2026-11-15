package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.camera.view.PreviewView
import com.example.connectivity.BluetoothDiscoveryManager
import com.example.connectivity.ExternalDisplayController
import com.example.connectivity.MotionXStreamingServer
import com.example.connectivity.NsdDiscoveryManager
import com.example.connectivity.QrCodeUtils
import com.example.data.db.MotionXDatabase
import com.example.data.prefs.SettingsDataStore
import com.example.data.repo.MotionXRepository
import com.example.drawing.AirDrawingExporter
import com.example.model.AirDrawingPalette
import com.example.model.AirPoint3D
import com.example.model.AirShapeTemplate
import com.example.model.AirStroke3D
import com.example.model.CameraResolutionPreset
import com.example.model.ConnectedSessionInfo
import com.example.model.ConnectionStatus
import com.example.model.CustomGameProject
import com.example.model.DiscoveredDevice
import com.example.model.DiscoverySource
import com.example.model.DualHandTrackingFrame
import com.example.model.GameActionTarget
import com.example.model.GameRuntimeState
import com.example.model.GestureMappingRule
import com.example.model.GestureType
import com.example.model.HandSide
import com.example.model.HandTelemetry
import com.example.model.MotionSettingsState
import com.example.model.ObjectTransformState
import com.example.model.SavedAirDrawing
import com.example.model.SdkEventLog
import com.example.model.TargetDeviceType
import com.example.model.VirtualObject3D
import com.example.model.VirtualObjectCategory
import com.example.runtime.GameStorageManager
import com.example.vision.HandTrackingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.random.Random

class MotionXViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = MotionXDatabase.getInstance(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val repository = MotionXRepository(database, settingsDataStore)
    private val storageManager = GameStorageManager(context)

    val handTrackingEngine = HandTrackingEngine(context)
    val bluetoothManager = BluetoothDiscoveryManager(context)
    val nsdManager = NsdDiscoveryManager(context)
    val streamingServer = MotionXStreamingServer(storageManager)
    val externalDisplayController = ExternalDisplayController(context)

    val settingsState: StateFlow<MotionSettingsState> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MotionSettingsState())

    val customGames: StateFlow<List<CustomGameProject>> = repository.gamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gestureMappings: StateFlow<List<GestureMappingRule>> = repository.gestureMappingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedDrawings: StateFlow<List<SavedAirDrawing>> = repository.savedDrawingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pairedDevices: StateFlow<List<DiscoveredDevice>> = repository.pairedDevicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customObjects3D: StateFlow<List<VirtualObject3D>> = repository.customObjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackingFrame: StateFlow<DualHandTrackingFrame> = handTrackingEngine.trackingFrame
    val trackingError: StateFlow<String?> = handTrackingEngine.engineError

    private val _selectedGameId = MutableStateFlow<String?>(null)
    val selectedGameId: StateFlow<String?> = _selectedGameId.asStateFlow()

    private val _runtimeState = MutableStateFlow(GameRuntimeState.STOPPED)
    val runtimeState: StateFlow<GameRuntimeState> = _runtimeState.asStateFlow()

    private val _previewOnExternalTv = MutableStateFlow(false)
    val previewOnExternalTv: StateFlow<Boolean> = _previewOnExternalTv.asStateFlow()

    private val _latestWebBridgeMessage = MutableStateFlow<Pair<String, String>?>(null)
    val latestWebBridgeMessage: StateFlow<Pair<String, String>?> = _latestWebBridgeMessage.asStateFlow()

    private val _sdkLogs = MutableStateFlow<List<SdkEventLog>>(emptyList())
    val sdkLogs: StateFlow<List<SdkEventLog>> = _sdkLogs.asStateFlow()

    private val _sessionInfo = MutableStateFlow(
        ConnectedSessionInfo(
            pairingPin = generatePin()
        )
    )
    val sessionInfo: StateFlow<ConnectedSessionInfo> = _sessionInfo.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Air Drawing Studio state
    private val _airStrokes = MutableStateFlow<List<AirStroke3D>>(emptyList())
    val airStrokes: StateFlow<List<AirStroke3D>> = _airStrokes.asStateFlow()

    private val _redoStrokes = MutableStateFlow<List<AirStroke3D>>(emptyList())
    val redoStrokes: StateFlow<List<AirStroke3D>> = _redoStrokes.asStateFlow()

    private val _airDrawActiveHand = MutableStateFlow(HandSide.RIGHT)
    val airDrawActiveHand: StateFlow<HandSide> = _airDrawActiveHand.asStateFlow()

    private val _airDrawIs3D = MutableStateFlow(true)
    val airDrawIs3D: StateFlow<Boolean> = _airDrawIs3D.asStateFlow()

    private val _airDrawColorArgb = MutableStateFlow(AirDrawingPalette.first().second.value.toLong())
    val airDrawColorArgb: StateFlow<Long> = _airDrawColorArgb.asStateFlow()

    private val _airDrawBrushSize = MutableStateFlow(8f)
    val airDrawBrushSize: StateFlow<Float> = _airDrawBrushSize.asStateFlow()

    private val _airDrawFingerTrackingEnabled = MutableStateFlow(true)
    val airDrawFingerTrackingEnabled: StateFlow<Boolean> = _airDrawFingerTrackingEnabled.asStateFlow()

    private var activeAirStrokeId: Long? = null
    private var lastGestureBroadcastMs = 0L
    private var lastHandBroadcastMs = 0L

    // 3D Weapon & Object Library state
    val builtInObjects3D: List<VirtualObject3D> = listOf(
        VirtualObject3D(
            id = "obj_ak47_cyber",
            name = "AK-47 Cyber Tactical",
            subtitle = "Virtual Assault Rifle • Modular Rail",
            category = VirtualObjectCategory.RIFLE,
            geometryPreset = "ak47",
            accentHex = 0xFF00F0FF
        ),
        VirtualObject3D(
            id = "obj_pulse_pistol",
            name = "Pulse Pistol MK-IV",
            subtitle = "Virtual Sidearm • Compact Kinetic",
            category = VirtualObjectCategory.SIDEARM,
            geometryPreset = "pistol",
            accentHex = 0xFF38BDF8
        ),
        VirtualObject3D(
            id = "obj_quantum_katana",
            name = "Quantum Katana",
            subtitle = "Virtual Plasma Blade • Dual Edge",
            category = VirtualObjectCategory.MELEE,
            geometryPreset = "sword",
            accentHex = 0xFF8B5CF6
        ),
        VirtualObject3D(
            id = "obj_aegis_rifle",
            name = "Aegis Plasma Launcher",
            subtitle = "Futuristic Energy Rifle",
            category = VirtualObjectCategory.FUTURISTIC,
            geometryPreset = "futuristic",
            accentHex = 0xFFEC4899
        ),
        VirtualObject3D(
            id = "obj_holo_shield",
            name = "Hex Holo-Shield",
            subtitle = "Interactive Defensive Barrier",
            category = VirtualObjectCategory.INTERACTIVE,
            geometryPreset = "shield",
            accentHex = 0xFF10B981
        ),
        VirtualObject3D(
            id = "obj_gravity_orb",
            name = "Gravity Core Orb",
            subtitle = "Interactive Physics Artifact",
            category = VirtualObjectCategory.INTERACTIVE,
            geometryPreset = "orb",
            accentHex = 0xFFF59E0B
        )
    )

    private val _selectedObjectId = MutableStateFlow(builtInObjects3D.first().id)
    val selectedObjectId: StateFlow<String> = _selectedObjectId.asStateFlow()

    private val _objectTransform = MutableStateFlow(ObjectTransformState())
    val objectTransform: StateFlow<ObjectTransformState> = _objectTransform.asStateFlow()

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = combine(
        bluetoothManager.devices,
        nsdManager.wifiDevices,
        pairedDevices
    ) { bt, wifi, saved ->
        val externalDisplays = externalDisplayController.listExternalDisplays()
        (externalDisplays + wifi + bt + saved).distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastPairedDeviceCandidate: DiscoveredDevice? = null
    private var lastActiveActionLabel = MutableStateFlow("Ready")
    val activeActionLabel: StateFlow<String> = lastActiveActionLabel.asStateFlow()

    private var serverInitJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultGestureMappings()
        }
        viewModelScope.launch {
            settingsState.collect { cfg ->
                handTrackingEngine.updateSettings(cfg)
                startLocalStreamingServices(cfg)
            }
        }
        viewModelScope.launch {
            streamingServer.connectedClients.collect { count ->
                val current = _sessionInfo.value
                val newStatus = if (count > 0) ConnectionStatus.CONNECTED else current.status
                _sessionInfo.value = current.copy(
                    connectedClientsCount = count,
                    status = newStatus,
                    latencyMs = if (count > 0) 7 else current.latencyMs
                )
            }
        }
        viewModelScope.launch {
            trackingFrame.collect { frame ->
                handleLiveTrackingFrame(frame)
            }
        }
    }

    private fun startLocalStreamingServices(cfg: MotionSettingsState) {
        serverInitJob?.cancel()
        serverInitJob = viewModelScope.launch(Dispatchers.IO) {
            val pin = _sessionInfo.value.pairingPin.ifBlank { generatePin() }
            streamingServer.start(cfg.httpPort, cfg.wsPort, pin)
            delay(250)
            nsdManager.registerControllerService(streamingServer.getHttpPort())
            _sessionInfo.value = _sessionInfo.value.copy(
                pairingPin = pin,
                localServerUrl = streamingServer.getLanReceiverUrl(pin),
                wsStreamUrl = streamingServer.getWsStreamUrl(),
                transportChannel = if (cfg.enableWebRtcDataChannel) {
                    "Local Wi-Fi WebSocket + WebRTC DataChannel"
                } else {
                    "Local Wi-Fi WebSocket"
                }
            )
        }
    }

    fun scanNearbyDevices() {
        _sessionInfo.value = _sessionInfo.value.copy(status = ConnectionStatus.DISCOVERING)
        if (settingsState.value.enableBluetoothDiscovery) {
            bluetoothManager.startDiscovery()
        }
        nsdManager.startWifiDiscovery()
        viewModelScope.launch {
            delay(3000)
            if (_sessionInfo.value.status == ConnectionStatus.DISCOVERING) {
                _sessionInfo.value = _sessionInfo.value.copy(
                    status = if (_sessionInfo.value.device != null) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
                )
            }
        }
    }

    fun connectToDevice(device: DiscoveredDevice, enteredPin: String? = null) {
        val pin = enteredPin?.takeIf { it.isNotBlank() } ?: _sessionInfo.value.pairingPin
        streamingServer.updatePairingPin(pin)
        lastPairedDeviceCandidate = device

        viewModelScope.launch {
            _sessionInfo.value = _sessionInfo.value.copy(
                device = device,
                pairingPin = pin,
                status = ConnectionStatus.PAIRING,
                lastError = null
            )

            // Handle physical/wireless Android Presentation Display directly if selected
            var externalDisplayed = false
            if (device.source == DiscoverySource.HDMI_CAST) {
                val displayId = device.id.removePrefix("display_").toIntOrNull()
                val game = getSelectedGame()
                if (displayId != null && game != null) {
                    val url = streamingServer.getLoopbackGameUrl(game.id, game.entryPoint)
                    externalDisplayed = externalDisplayController.showGameOnDisplay(displayId, url)
                }
            }

            repository.savePairedDevice(device)
            _sessionInfo.value = _sessionInfo.value.copy(
                device = device.copy(isPaired = true),
                status = ConnectionStatus.CONNECTED,
                pairingPin = pin,
                localServerUrl = streamingServer.getLanReceiverUrl(pin),
                wsStreamUrl = streamingServer.getWsStreamUrl(),
                latencyMs = if (externalDisplayed) 4 else 8,
                packetsPerSecond = 60,
                isDisplayMirroredToTv = externalDisplayed
            )
            broadcastConnectionEvent()
            _statusBanner.value = "Paired with ${device.name} (PIN $pin)"
        }
    }

    fun connectWithManualPin(pin: String, hostOrName: String, deviceType: TargetDeviceType) {
        val cleanPin = pin.filter { it.isDigit() }.take(6)
        if (cleanPin.length < 4) {
            _statusBanner.value = "Enter at least a 4–6 digit pairing PIN"
            return
        }
        val cleanHost = hostOrName.trim().ifBlank { streamingServer.getLocalIpAddress() }
        val device = DiscoveredDevice(
            id = "manual_${cleanHost}_$cleanPin",
            name = if (hostOrName.isBlank()) "${deviceType.label} ($cleanPin)" else hostOrName,
            address = cleanHost,
            port = streamingServer.getWsPort(),
            type = deviceType,
            source = DiscoverySource.MANUAL_PIN,
            supportsWebRtc = settingsState.value.enableWebRtcDataChannel,
            isPaired = true
        )
        connectToDevice(device, cleanPin)
    }

    fun handleScannedQrCode(rawQr: String) {
        val parsed = QrCodeUtils.parsePairingPayload(rawQr)
        if (parsed != null) {
            val (device, pin) = parsed
            connectToDevice(device, pin.ifBlank { _sessionInfo.value.pairingPin })
        } else {
            _statusBanner.value = "QR scanned: $rawQr"
        }
    }

    fun disconnectDevice() {
        externalDisplayController.dismissPresentation()
        _previewOnExternalTv.value = false
        val last = _sessionInfo.value.device ?: lastPairedDeviceCandidate
        if (last != null) lastPairedDeviceCandidate = last
        _sessionInfo.value = _sessionInfo.value.copy(
            status = ConnectionStatus.DISCONNECTED,
            packetsPerSecond = 0,
            isDisplayMirroredToTv = false
        )
        broadcastConnectionEvent()
        _statusBanner.value = "Disconnected from target display"
    }

    fun reconnectDevice() {
        val candidate = _sessionInfo.value.device ?: lastPairedDeviceCandidate ?: pairedDevices.value.firstOrNull()
        if (candidate == null) {
            _statusBanner.value = "No previous device found. Scan or enter PIN first."
            return
        }
        _sessionInfo.value = _sessionInfo.value.copy(status = ConnectionStatus.RECONNECTING)
        connectToDevice(candidate, _sessionInfo.value.pairingPin)
    }

    fun regeneratePin() {
        val newPin = generatePin()
        streamingServer.updatePairingPin(newPin)
        _sessionInfo.value = _sessionInfo.value.copy(
            pairingPin = newPin,
            localServerUrl = streamingServer.getLanReceiverUrl(newPin)
        )
    }

    // Custom Game Studio actions
    fun uploadGameUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = storageManager.importGameFromUris(uris)
            result.onSuccess { project ->
                repository.saveCustomGame(project)
                _selectedGameId.value = project.id
                streamingServer.setActiveGame(project.id, project.entryPoint)
                _statusBanner.value = "Uploaded \"${project.title}\" (${project.files.size} files, entry: ${project.entryPoint})"
            }.onFailure { err ->
                _statusBanner.value = "Upload error: ${err.localizedMessage}"
            }
        }
    }

    fun installSdkStarterArena() {
        viewModelScope.launch(Dispatchers.IO) {
            val project = storageManager.installCyberArenaTemplate()
            repository.saveCustomGame(project)
            _selectedGameId.value = project.id
            streamingServer.setActiveGame(project.id, project.entryPoint)
            _statusBanner.value = "Installed Cyber Motion Arena SDK Demo"
        }
    }

    fun selectGame(game: CustomGameProject) {
        _selectedGameId.value = game.id
        streamingServer.setActiveGame(game.id, game.entryPoint)
    }

    fun updateGameConfig(game: CustomGameProject, title: String, orientation: String, sensitivity: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCustomGame(
                game.copy(
                    title = title.ifBlank { game.title },
                    targetOrientation = orientation,
                    sensitivityMultiplier = sensitivity.coerceIn(0.3f, 2.5f)
                )
            )
            _statusBanner.value = "Saved configuration for $title"
        }
    }

    fun deleteGame(game: CustomGameProject) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedGameId.value == game.id) {
                _runtimeState.value = GameRuntimeState.STOPPED
                _selectedGameId.value = null
            }
            storageManager.deleteGameFiles(game.id)
            repository.deleteCustomGame(game.id)
        }
    }

    fun getSelectedGame(): CustomGameProject? {
        val list = customGames.value
        return list.firstOrNull { it.id == _selectedGameId.value } ?: list.firstOrNull()
    }

    fun getGameRuntimeUrl(game: CustomGameProject?): String {
        if (game == null) return ""
        return streamingServer.getLoopbackGameUrl(game.id, game.entryPoint)
    }

    fun startGameRuntime() {
        val game = getSelectedGame() ?: return
        _selectedGameId.value = game.id
        streamingServer.setActiveGame(game.id, game.entryPoint)
        _runtimeState.value = GameRuntimeState.RUNNING
        val dev = _sessionInfo.value.device
        if (dev != null && dev.source == DiscoverySource.HDMI_CAST) {
            val displayId = dev.id.removePrefix("display_").toIntOrNull()
            if (displayId != null) {
                externalDisplayController.showGameOnDisplay(displayId, getGameRuntimeUrl(game))
            }
        }
    }

    fun stopGameRuntime() {
        _runtimeState.value = GameRuntimeState.STOPPED
        externalDisplayController.dismissPresentation()
    }

    fun toggleDisplayTarget(externalTv: Boolean) {
        _previewOnExternalTv.value = externalTv
        val game = getSelectedGame()
        if (game != null) {
            streamingServer.setActiveGame(game.id, game.entryPoint)
            _sessionInfo.value = _sessionInfo.value.copy(isDisplayMirroredToTv = externalTv)
        }
    }

    // Camera & Hand Tracking controls
    fun startCameraTracking(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        handTrackingEngine.startCamera(lifecycleOwner, previewView)
    }

    fun stopCameraTracking() {
        handTrackingEngine.stopCamera()
    }

    fun triggerCalibratedTestGesture(gesture: GestureType, side: HandSide = HandSide.RIGHT) {
        handTrackingEngine.publishCalibratedTestGesture(gesture, side)
    }

    // Gesture Config actions
    fun updateGestureMapping(rule: GestureMappingRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveGestureMapping(rule)
        }
    }

    fun resetGestureMappings() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetDefaultGestureMappings()
            _statusBanner.value = "Reset gesture mappings to defaults"
        }
    }

    // Settings actions
    fun updateSettings(transform: (MotionSettingsState) -> MotionSettingsState) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(transform)
        }
    }

    // Air Drawing Studio actions
    fun setAirDrawHand(side: HandSide) { _airDrawActiveHand.value = side }
    fun setAirDraw3DMode(enabled: Boolean) { _airDrawIs3D.value = enabled }
    fun setAirDrawColor(argb: Long) { _airDrawColorArgb.value = argb }
    fun setAirDrawBrushSize(size: Float) { _airDrawBrushSize.value = size }
    fun setAirDrawFingerTracking(enabled: Boolean) { _airDrawFingerTrackingEnabled.value = enabled }

    fun addTouchAirPoint(x: Float, y: Float, isStart: Boolean) {
        val z = if (_airDrawIs3D.value) ((0.5f - y) * 0.25f) else 0f
        appendAirPoint(AirPoint3D(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f), z), isStart)
    }

    private fun appendAirPoint(point: AirPoint3D, forceNewStroke: Boolean = false) {
        val current = _airStrokes.value.toMutableList()
        if (forceNewStroke || activeAirStrokeId == null || current.none { it.id == activeAirStrokeId }) {
            val id = System.nanoTime()
            activeAirStrokeId = id
            current.add(
                AirStroke3D(
                    id = id,
                    points = listOf(point),
                    colorArgb = _airDrawColorArgb.value,
                    brushSizeDp = _airDrawBrushSize.value,
                    is3DMode = _airDrawIs3D.value,
                    handUsed = _airDrawActiveHand.value
                )
            )
            _redoStrokes.value = emptyList()
        } else {
            val idx = current.indexOfLast { it.id == activeAirStrokeId }
            if (idx >= 0) {
                val stroke = current[idx]
                val last = stroke.points.lastOrNull()
                if (last == null ||
                    kotlin.math.abs(last.x - point.x) > 0.004f ||
                    kotlin.math.abs(last.y - point.y) > 0.004f
                ) {
                    current[idx] = stroke.copy(points = stroke.points + point)
                }
            }
        }
        _airStrokes.value = current
        broadcastAirDrawUpdate(current.lastOrNull()?.points.orEmpty())
    }

    fun endAirStroke() {
        activeAirStrokeId = null
    }

    fun insertAirShapePreset(template: AirShapeTemplate) {
        val pts = AirDrawingExporter.generateShapePreset(template.name)
        if (pts.isEmpty()) return
        val newStroke = AirStroke3D(
            id = System.nanoTime(),
            points = pts,
            colorArgb = _airDrawColorArgb.value,
            brushSizeDp = _airDrawBrushSize.value,
            is3DMode = true,
            handUsed = _airDrawActiveHand.value
        )
        _airStrokes.value = _airStrokes.value + newStroke
        _redoStrokes.value = emptyList()
        broadcastAirDrawUpdate(pts)
    }

    fun undoAirStroke() {
        val current = _airStrokes.value
        if (current.isEmpty()) return
        _redoStrokes.value = _redoStrokes.value + current.last()
        _airStrokes.value = current.dropLast(1)
        activeAirStrokeId = null
    }

    fun redoAirStroke() {
        val redo = _redoStrokes.value
        if (redo.isEmpty()) return
        _airStrokes.value = _airStrokes.value + redo.last()
        _redoStrokes.value = redo.dropLast(1)
    }

    fun clearAirDrawing() {
        _airStrokes.value = emptyList()
        _redoStrokes.value = emptyList()
        activeAirStrokeId = null
        broadcastAirDrawUpdate(emptyList())
    }

    fun saveAndExportAirDrawing(title: String, alsoSendToLibrary: Boolean = true) {
        val strokes = _airStrokes.value
        if (strokes.isEmpty()) {
            _statusBanner.value = "Draw in the air or add a shape first"
            return
        }
        val cleanTitle = title.trim().ifBlank { "Air Creation #${savedDrawings.value.size + 1}" }
        viewModelScope.launch(Dispatchers.IO) {
            val objFile = AirDrawingExporter.exportToObj(context, cleanTitle, strokes)
            val gltfFile = AirDrawingExporter.exportToGltf(context, cleanTitle, strokes)
            val json = AirDrawingExporter.strokesToJson(strokes)
            val drawing = SavedAirDrawing(
                id = "draw_${UUID.randomUUID().toString().take(8)}",
                title = cleanTitle,
                strokeCount = strokes.size,
                pointCount = strokes.sumOf { it.points.size },
                strokesJson = json,
                exportedObjPath = objFile.absolutePath,
                exportedGltfPath = gltfFile.absolutePath
            )
            repository.saveDrawing(drawing)
            if (alsoSendToLibrary) {
                val obj3d = VirtualObject3D(
                    id = "air_obj_${drawing.id}",
                    name = cleanTitle,
                    subtitle = "3D Air Creation (${drawing.strokeCount} strokes)",
                    category = VirtualObjectCategory.AIR_DRAWN,
                    geometryPreset = "air_custom",
                    accentHex = strokes.last().colorArgb,
                    customModelPath = gltfFile.absolutePath
                )
                repository.saveCustomObject3D(obj3d)
                _selectedObjectId.value = obj3d.id
            }
            _statusBanner.value = "Saved & Exported 3D OBJ + glTF: ${objFile.name}"
        }
    }

    fun deleteSavedDrawing(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDrawing(id)
            _statusBanner.value = "Removed saved 3D drawing"
        }
    }

    fun shareExportedFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        val file = File(filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export 3D Creation").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // 3D Weapon / Object Library actions
    fun selectObject3D(obj: VirtualObject3D) {
        _selectedObjectId.value = obj.id
    }

    fun importGlbObjectUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.import3DModelUri(uri).onSuccess { (name, path) ->
                val obj = VirtualObject3D(
                    id = "glb_${UUID.randomUUID().toString().take(8)}",
                    name = name,
                    subtitle = "Uploaded GLB / glTF Asset",
                    category = VirtualObjectCategory.CUSTOM_GLB,
                    geometryPreset = "custom_glb",
                    accentHex = 0xFF00F0FF,
                    customModelPath = path
                )
                repository.saveCustomObject3D(obj)
                _selectedObjectId.value = obj.id
                _statusBanner.value = "Imported 3D model: $name"
            }.onFailure { err ->
                _statusBanner.value = "3D Import error: ${err.localizedMessage}"
            }
        }
    }

    fun updateObjectTransform(transform: (ObjectTransformState) -> ObjectTransformState) {
        _objectTransform.value = transform(_objectTransform.value)
    }

    fun placeSelectedObjectInGame() {
        val allObjects = builtInObjects3D + customObjects3D.value
        val obj = allObjects.firstOrNull { it.id == _selectedObjectId.value } ?: builtInObjects3D.first()
        val t = _objectTransform.value.copy(isPlacedInGame = true)
        _objectTransform.value = t

        val payload = JSONObject().apply {
            put("objectId", obj.id)
            put("name", obj.name)
            put("type", obj.category.name)
            put("preset", obj.geometryPreset)
            put("position", JSONObject().apply {
                put("x", t.positionX)
                put("y", t.positionY)
                put("z", t.positionZ)
            })
            put("rotation", JSONObject().apply {
                put("pitch", t.rotationPitch)
                put("yaw", t.rotationYaw)
                put("roll", t.rotationRoll)
            })
            put("scale", t.scale)
            put("action", "PLACE_IN_GAME")
        }.toString()

        dispatchToRuntimeAndTv("objectAction", payload)
        _statusBanner.value = "Placed ${obj.name} into active game environment"
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun onGameSdkFeedback(msg: String) {
        appendSdkLog("feedback", msg)
    }

    // Core Real-Time Telemetry -> SDK / Game / AirDraw / 3D Transform Pipeline
    private fun handleLiveTrackingFrame(frame: DualHandTrackingFrame) {
        val now = System.currentTimeMillis()
        if (frame.leftHand == null && frame.rightHand == null) {
            endAirStroke()
            return
        }

        // 1. Air Drawing engagement when index-pointing
        if (_airDrawFingerTrackingEnabled.value) {
            val drawHand = if (_airDrawActiveHand.value == HandSide.RIGHT) frame.rightHand else frame.leftHand
            if (drawHand != null && (drawHand.isIndexPointing || drawHand.isPinching)) {
                val depthZ = if (_airDrawIs3D.value) (-drawHand.indexTip.z * 2.5f).coerceIn(-0.6f, 0.6f) else 0f
                appendAirPoint(AirPoint3D(drawHand.indexTip.x, drawHand.indexTip.y, depthZ))
            } else {
                endAirStroke()
            }
        }

        // 2. Hand-controlled 3D Object manipulation
        if (_objectTransform.value.handControlEnabled) {
            val driver = frame.rightHand ?: frame.leftHand
            if (driver != null) {
                val twoHandScale = frame.handsDistance?.let { (it * 2.2f).coerceIn(0.45f, 2.2f) }
                _objectTransform.value = _objectTransform.value.copy(
                    positionX = ((driver.palmCenter.x - 0.5f) * 2.0f).coerceIn(-1f, 1f),
                    positionY = ((0.5f - driver.palmCenter.y) * 2.0f).coerceIn(-1f, 1f),
                    rotationYaw = (driver.palmCenter.x - 0.5f) * 180f,
                    rotationRoll = driver.palmRollDegrees,
                    scale = twoHandScale ?: _objectTransform.value.scale
                )
            }
        }

        // 3. Throttle handMove stream to ~30–60Hz
        if (now - lastHandBroadcastMs >= 28L) {
            lastHandBroadcastMs = now
            val handPayload = buildHandMoveJson(frame)
            dispatchToRuntimeAndTv("handMove", handPayload)
        }

        // 4. Evaluate configurable Left & Right Gesture Mappings
        val activeGesture = frame.combinedGesture
        if (activeGesture != GestureType.NONE && now - lastGestureBroadcastMs >= 160L) {
            lastGestureBroadcastMs = now
            val activeSide = when {
                frame.rightHand?.activeGesture == activeGesture -> HandSide.RIGHT
                frame.leftHand?.activeGesture == activeGesture -> HandSide.LEFT
                else -> HandSide.RIGHT
            }
            val matchedRule = gestureMappings.value.firstOrNull {
                it.enabled && it.handSide == activeSide && it.gesture == activeGesture
            }
            val actionName = matchedRule?.customSdkEvent ?: activeGesture.name.lowercase()
            val mappedKey = matchedRule?.actionTarget?.keyCode ?: ""
            lastActiveActionLabel.value = "${activeSide.name}: ${activeGesture.displayName} → ${matchedRule?.actionTarget?.label ?: actionName}"

            val gestureJson = JSONObject().apply {
                put("name", activeGesture.name)
                put("label", activeGesture.displayName)
                put("hand", activeSide.name.lowercase())
                put("confidence", (frame.rightHand ?: frame.leftHand)?.confidence ?: 0.9f)
                put("action", actionName)
                put("mappedControl", mappedKey)
                put("timestamp", now)
            }.toString()

            dispatchToRuntimeAndTv("gesture", gestureJson)
            appendSdkLog("gesture", "${activeSide.name} ${activeGesture.name} ($mappedKey)")
        }
    }

    private fun broadcastAirDrawUpdate(points: List<AirPoint3D>) {
        val ptsArr = JSONArray()
        points.takeLast(80).forEach { p ->
            ptsArr.put(JSONObject().apply {
                put("x", p.x)
                put("y", p.y)
                put("z", p.z)
            })
        }
        val payload = JSONObject().apply {
            put("points", ptsArr)
            put("activeColor", "#%08X".format(_airDrawColorArgb.value))
            put("brushSize", _airDrawBrushSize.value)
            put("mode3D", _airDrawIs3D.value)
            put("timestamp", System.currentTimeMillis())
        }.toString()
        dispatchToRuntimeAndTv("airDraw", payload)
    }

    private fun broadcastConnectionEvent() {
        val s = _sessionInfo.value
        val payload = JSONObject().apply {
            put("status", s.status.name)
            put("deviceName", s.device?.name ?: "None")
            put("pin", s.pairingPin)
            put("latencyMs", s.latencyMs)
            put("transport", s.transportChannel)
        }.toString()
        dispatchToRuntimeAndTv("connection", payload)
    }

    private fun dispatchToRuntimeAndTv(channel: String, payloadJson: String) {
        _latestWebBridgeMessage.value = channel to payloadJson
        streamingServer.broadcastChannelEvent(channel, payloadJson)
        externalDisplayController.dispatchMotion(channel, payloadJson)
    }

    private fun buildHandMoveJson(frame: DualHandTrackingFrame): String {
        fun handObj(h: HandTelemetry?): Any {
            if (h == null) return JSONObject.NULL
            val lms = JSONArray()
            h.landmarks.forEach { pt ->
                lms.put(JSONObject().apply {
                    put("x", pt.x)
                    put("y", pt.y)
                    put("z", pt.z)
                })
            }
            return JSONObject().apply {
                put("side", h.side.name.lowercase())
                put("confidence", h.confidence)
                put("palmCenter", JSONObject().apply {
                    put("x", h.palmCenter.x)
                    put("y", h.palmCenter.y)
                    put("z", h.palmCenter.z)
                })
                put("wrist", JSONObject().apply {
                    put("x", h.wrist.x)
                    put("y", h.wrist.y)
                    put("z", h.wrist.z)
                })
                put("palmRoll", h.palmRollDegrees)
                put("pinchDistance", h.pinchDistance)
                put("isOpenPalm", h.isOpenPalm)
                put("isClosedFist", h.isClosedFist)
                put("isPinching", h.isPinching)
                put("landmarks", lms)
            }
        }
        return JSONObject().apply {
            put("leftHand", handObj(frame.leftHand))
            put("rightHand", handObj(frame.rightHand))
            put("handsDistance", frame.handsDistance ?: JSONObject.NULL)
            put("fps", frame.fps)
            put("timestamp", frame.timestampMs)
        }.toString()
    }

    private fun appendSdkLog(channel: String, preview: String) {
        val entry = SdkEventLog(System.nanoTime(), channel, preview)
        _sdkLogs.value = (listOf(entry) + _sdkLogs.value).take(24)
    }

    private fun generatePin(): String = (100000 + Random.nextInt(899999)).toString()

    override fun onCleared() {
        handTrackingEngine.release()
        bluetoothManager.release()
        nsdManager.stopWifiDiscovery()
        nsdManager.unregisterService()
        externalDisplayController.dismissPresentation()
        streamingServer.stop()
        super.onCleared()
    }
}
