package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.ConnectionStatus
import com.example.ui.screens.AirDrawingScreen
import com.example.ui.screens.ConnectionSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceConnectionScreen
import com.example.ui.screens.GamePreviewScreen
import com.example.ui.screens.GameStudioScreen
import com.example.ui.screens.GestureConfigScreen
import com.example.ui.screens.MotionTrackingScreen
import com.example.ui.screens.ObjectLibraryScreen
import com.example.ui.screens.SdkDocsScreen
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyberGridBackground
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusPill
import com.example.viewmodel.MotionXViewModel

sealed class MotionRoute(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : MotionRoute("dashboard", "Home", Icons.Default.Dashboard)
    data object DeviceConnection : MotionRoute("device_connection", "Connect", Icons.Default.CastConnected)
    data object GameStudio : MotionRoute("game_studio", "Studio", Icons.Default.UploadFile)
    data object GamePreview : MotionRoute("game_preview", "Preview", Icons.Default.Gamepad)
    data object MotionTracking : MotionRoute("motion_tracking", "Vision", Icons.Default.CameraAlt)
    data object GestureConfig : MotionRoute("gesture_config", "Gestures", Icons.Default.Tune)
    data object AirDrawing : MotionRoute("air_drawing", "Air Draw", Icons.Default.Brush)
    data object ObjectLibrary : MotionRoute("object_library", "3D Arsenal", Icons.Default.ViewInAr)
    data object ConnectionSettings : MotionRoute("connection_settings", "Settings", Icons.Default.Settings)
    data object SdkDocs : MotionRoute("sdk_docs", "SDK Docs", Icons.AutoMirrored.Filled.MenuBook)
}

private val primaryNavItems = listOf(
    MotionRoute.Dashboard,
    MotionRoute.DeviceConnection,
    MotionRoute.GameStudio,
    MotionRoute.GamePreview,
    MotionRoute.MotionTracking
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionXRootApp(viewModel: MotionXViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteStr = backStackEntry?.destination?.route ?: MotionRoute.Dashboard.route
    val snackbarHostState = remember { SnackbarHostState() }

    val sessionInfo by viewModel.sessionInfo.collectAsStateWithLifecycle()
    val trackingFrame by viewModel.trackingFrame.collectAsStateWithLifecycle()
    val trackingError by viewModel.trackingError.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val customGames by viewModel.customGames.collectAsStateWithLifecycle()
    val selectedGameId by viewModel.selectedGameId.collectAsStateWithLifecycle()
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()
    val previewOnExternalTv by viewModel.previewOnExternalTv.collectAsStateWithLifecycle()
    val latestWebBridgeMessage by viewModel.latestWebBridgeMessage.collectAsStateWithLifecycle()
    val activeActionLabel by viewModel.activeActionLabel.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isScanningBt by viewModel.bluetoothManager.isScanning.collectAsStateWithLifecycle()
    val btStatusNote by viewModel.bluetoothManager.statusNote.collectAsStateWithLifecycle()
    val gestureMappings by viewModel.gestureMappings.collectAsStateWithLifecycle()
    val airStrokes by viewModel.airStrokes.collectAsStateWithLifecycle()
    val redoStrokes by viewModel.redoStrokes.collectAsStateWithLifecycle()
    val airDrawHand by viewModel.airDrawActiveHand.collectAsStateWithLifecycle()
    val airDraw3D by viewModel.airDrawIs3D.collectAsStateWithLifecycle()
    val airDrawColor by viewModel.airDrawColorArgb.collectAsStateWithLifecycle()
    val airDrawBrush by viewModel.airDrawBrushSize.collectAsStateWithLifecycle()
    val airDrawFingerEnabled by viewModel.airDrawFingerTrackingEnabled.collectAsStateWithLifecycle()
    val savedDrawings by viewModel.savedDrawings.collectAsStateWithLifecycle()
    val customObjects3D by viewModel.customObjects3D.collectAsStateWithLifecycle()
    val selectedObjectId by viewModel.selectedObjectId.collectAsStateWithLifecycle()
    val objectTransform by viewModel.objectTransform.collectAsStateWithLifecycle()
    val sdkLogs by viewModel.sdkLogs.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    LaunchedEffect(statusBanner) {
        val msg = statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusBanner()
    }

    val activeGame = remember(customGames, selectedGameId) {
        customGames.firstOrNull { it.id == selectedGameId } ?: customGames.firstOrNull()
    }

    fun navigateTo(route: MotionRoute) {
        val isPrimary = primaryNavItems.any { it.route == route.route }
        navController.navigate(route.route) {
            if (isPrimary) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            } else {
                launchSingleTop = true
            }
        }
    }

    val isPrimaryScreen = primaryNavItems.any { it.route == currentRouteStr }

    CyberGridBackground(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = PitchBlack.copy(alpha = 0f),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DeepObsidian.copy(alpha = 0.92f),
                            titleContentColor = PureWhite
                        ),
                        navigationIcon = {
                            if (!isPrimaryScreen) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.testTag("top_back_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = ElectricCyan
                                    )
                                }
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MOTION X",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                StatusPill(
                                    label = if (sessionInfo.status == ConnectionStatus.CONNECTED) "LINKED" else "LOCAL",
                                    color = if (sessionInfo.status == ConnectionStatus.CONNECTED) EmeraldSignal else ElectricCyan
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navigateTo(MotionRoute.AirDrawing) },
                                modifier = Modifier.testTag("top_air_draw_action")
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = "Air Drawing Studio", tint = ElectricCyan)
                            }
                            IconButton(
                                onClick = { navigateTo(MotionRoute.ObjectLibrary) },
                                modifier = Modifier.testTag("top_object_lib_action")
                            ) {
                                Icon(Icons.Default.ViewInAr, contentDescription = "3D Weapon Library", tint = PureWhite)
                            }
                            IconButton(
                                onClick = { navigateTo(MotionRoute.ConnectionSettings) },
                                modifier = Modifier.testTag("top_settings_action")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = CoolSlate)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = DeepObsidian,
                            contentColor = PureWhite
                        ) {
                            primaryNavItems.forEach { item ->
                                val selected = currentRouteStr == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item) },
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    label = { Text(item.title, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = ElectricCyan.copy(alpha = 0.16f),
                                        unselectedIconColor = CoolSlate,
                                        unselectedTextColor = CoolSlate
                                    ),
                                    modifier = Modifier.testTag("nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(containerColor = DeepObsidian) {
                            primaryNavItems.forEach { item ->
                                val selected = currentRouteStr == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item) },
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    label = { Text(item.title) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = ElectricCyan.copy(alpha = 0.16f),
                                        unselectedIconColor = CoolSlate,
                                        unselectedTextColor = CoolSlate
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = MotionRoute.Dashboard.route
                        ) {
                            composable(MotionRoute.Dashboard.route) {
                                DashboardScreen(
                                    sessionInfo = sessionInfo,
                                    trackingFrame = trackingFrame,
                                    activeGame = activeGame,
                                    gamesCount = customGames.size,
                                    drawingsCount = savedDrawings.size,
                                    onNavigate = ::navigateTo
                                )
                            }
                            composable(MotionRoute.DeviceConnection.route) {
                                DeviceConnectionScreen(
                                    sessionInfo = sessionInfo,
                                    discoveredDevices = discoveredDevices,
                                    isScanningBluetooth = isScanningBt,
                                    bluetoothStatusNote = btStatusNote,
                                    requiredBtPermissions = viewModel.bluetoothManager.requiredPermissions(),
                                    hasBtPermissions = viewModel.bluetoothManager.hasPermissions(),
                                    onScanNearby = viewModel::scanNearbyDevices,
                                    onConnectDevice = { viewModel.connectToDevice(it) },
                                    onConnectManualPin = viewModel::connectWithManualPin,
                                    onQrScanned = viewModel::handleScannedQrCode,
                                    onDisconnect = viewModel::disconnectDevice,
                                    onReconnect = viewModel::reconnectDevice,
                                    onRegeneratePin = viewModel::regeneratePin
                                )
                            }
                            composable(MotionRoute.GameStudio.route) {
                                GameStudioScreen(
                                    games = customGames,
                                    selectedGameId = selectedGameId,
                                    onUploadGameUris = viewModel::uploadGameUris,
                                    onInstallSdkDemo = viewModel::installSdkStarterArena,
                                    onSelectGame = viewModel::selectGame,
                                    onUpdateGameConfig = viewModel::updateGameConfig,
                                    onDeleteGame = viewModel::deleteGame,
                                    onOpenPreview = { game ->
                                        viewModel.selectGame(game)
                                        viewModel.startGameRuntime()
                                        navigateTo(MotionRoute.GamePreview)
                                    }
                                )
                            }
                            composable(MotionRoute.GamePreview.route) {
                                GamePreviewScreen(
                                    activeGame = activeGame,
                                    gameUrl = viewModel.getGameRuntimeUrl(activeGame),
                                    runtimeState = runtimeState,
                                    previewOnExternalTv = previewOnExternalTv,
                                    sessionInfo = sessionInfo,
                                    trackingFrame = trackingFrame,
                                    activeActionLabel = activeActionLabel,
                                    latestWebBridgeMessage = latestWebBridgeMessage,
                                    onStartGame = viewModel::startGameRuntime,
                                    onStopGame = viewModel::stopGameRuntime,
                                    onToggleDisplayTarget = viewModel::toggleDisplayTarget,
                                    onPairWithPin = { pin ->
                                        viewModel.connectWithManualPin(
                                            pin,
                                            "",
                                            com.example.model.TargetDeviceType.SMART_TV
                                        )
                                    },
                                    onOpenTracking = { navigateTo(MotionRoute.MotionTracking) },
                                    onOpenStudio = { navigateTo(MotionRoute.GameStudio) },
                                    onTestGesture = viewModel::triggerCalibratedTestGesture,
                                    onSdkFeedback = viewModel::onGameSdkFeedback
                                )
                            }
                            composable(MotionRoute.MotionTracking.route) {
                                MotionTrackingScreen(
                                    trackingFrame = trackingFrame,
                                    trackingError = trackingError,
                                    settings = settingsState,
                                    onStartCamera = viewModel::startCameraTracking,
                                    onStopCamera = viewModel::stopCameraTracking,
                                    onToggleLens = { front ->
                                        viewModel.updateSettings { it.copy(useFrontCamera = front) }
                                    },
                                    onSelectResolution = { preset ->
                                        viewModel.updateSettings { it.copy(resolutionPreset = preset) }
                                    }
                                )
                            }
                            composable(MotionRoute.GestureConfig.route) {
                                GestureConfigScreen(
                                    rules = gestureMappings,
                                    activeActionLabel = activeActionLabel,
                                    onUpdateRule = viewModel::updateGestureMapping,
                                    onResetDefaults = viewModel::resetGestureMappings,
                                    onTestGesture = viewModel::triggerCalibratedTestGesture
                                )
                            }
                            composable(MotionRoute.AirDrawing.route) {
                                AirDrawingScreen(
                                    strokes = airStrokes,
                                    canRedo = redoStrokes.isNotEmpty(),
                                    activeHand = airDrawHand,
                                    is3DMode = airDraw3D,
                                    activeColorArgb = airDrawColor,
                                    brushSize = airDrawBrush,
                                    fingerTrackingEnabled = airDrawFingerEnabled,
                                    trackingFrame = trackingFrame,
                                    savedDrawings = savedDrawings,
                                    onSelectHand = viewModel::setAirDrawHand,
                                    onToggle3DMode = viewModel::setAirDraw3DMode,
                                    onSelectColor = viewModel::setAirDrawColor,
                                    onBrushSizeChange = viewModel::setAirDrawBrushSize,
                                    onToggleFingerTracking = viewModel::setAirDrawFingerTracking,
                                    onTouchDrawPoint = viewModel::addTouchAirPoint,
                                    onEndStroke = viewModel::endAirStroke,
                                    onInsertShapePreset = viewModel::insertAirShapePreset,
                                    onUndo = viewModel::undoAirStroke,
                                    onRedo = viewModel::redoAirStroke,
                                    onClear = viewModel::clearAirDrawing,
                                    onSaveAndExport = { title -> viewModel.saveAndExportAirDrawing(title, true) },
                                    onShareExport = viewModel::shareExportedFile,
                                    onDeleteSaved = viewModel::deleteSavedDrawing
                                )
                            }
                            composable(MotionRoute.ObjectLibrary.route) {
                                ObjectLibraryScreen(
                                    builtInObjects = viewModel.builtInObjects3D,
                                    customObjects = customObjects3D,
                                    selectedObjectId = selectedObjectId,
                                    transformState = objectTransform,
                                    onSelectObject = viewModel::selectObject3D,
                                    onImportGlbUri = viewModel::importGlbObjectUri,
                                    onUpdateTransform = viewModel::updateObjectTransform,
                                    onPlaceInGame = viewModel::placeSelectedObjectInGame
                                )
                            }
                            composable(MotionRoute.ConnectionSettings.route) {
                                ConnectionSettingsScreen(
                                    settings = settingsState,
                                    sessionInfo = sessionInfo,
                                    onUpdateSettings = viewModel::updateSettings
                                )
                            }
                            composable(MotionRoute.SdkDocs.route) {
                                SdkDocsScreen(
                                    sdkLogs = sdkLogs,
                                    onInstallAndLaunchDemo = {
                                        viewModel.installSdkStarterArena()
                                        navigateTo(MotionRoute.GamePreview)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
