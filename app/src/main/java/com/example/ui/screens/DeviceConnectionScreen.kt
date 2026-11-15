package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.connectivity.QrCodeUtils
import com.example.model.ConnectedSessionInfo
import com.example.model.ConnectionStatus
import com.example.model.DiscoveredDevice
import com.example.model.TargetDeviceType
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun DeviceConnectionScreen(
    sessionInfo: ConnectedSessionInfo,
    discoveredDevices: List<DiscoveredDevice>,
    isScanningBluetooth: Boolean,
    bluetoothStatusNote: String,
    requiredBtPermissions: Array<String>,
    hasBtPermissions: Boolean,
    onScanNearby: () -> Unit,
    onConnectDevice: (DiscoveredDevice) -> Unit,
    onConnectManualPin: (String, String, TargetDeviceType) -> Unit,
    onQrScanned: (String) -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onRegeneratePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf<TargetDeviceType?>(null) }
    var manualPin by remember { mutableStateOf("") }
    var manualHost by remember { mutableStateOf("") }
    var manualType by remember { mutableStateOf(TargetDeviceType.SMART_TV) }
    var showQrDisplayDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    val btPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onScanNearby()
    }

    val camPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showQrScannerDialog = true
    }

    val filteredDevices = remember(discoveredDevices, selectedFilter) {
        if (selectedFilter == null) discoveredDevices
        else discoveredDevices.filter { it.type == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("device_connection_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Device Connection Hub",
                subtitle = "Bluetooth discovery + Low-Latency Wi-Fi WebSocket / WebRTC channel"
            )
        }

        // Connected Status & Session Controls
        item {
            val isConnected = sessionInfo.status == ConnectionStatus.CONNECTED
            GlassPanel(
                highlighted = isConnected,
                accentColor = if (isConnected) EmeraldSignal else ElectricCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = sessionInfo.status.label,
                        color = if (isConnected) EmeraldSignal else ElectricCyan
                    )
                    Text(
                        text = "PIN: ${sessionInfo.pairingPin}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = sessionInfo.device?.name ?: "No External Display Paired",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite
                )
                Text(
                    text = if (sessionInfo.localServerUrl.isNotBlank()) {
                        "TV Browser Receiver: ${sessionInfo.localServerUrl}\nStream: ${sessionInfo.wsStreamUrl} • ${sessionInfo.transportChannel}"
                    } else {
                        "Starting local high-speed motion stream server..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                if (isConnected || sessionInfo.device != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clients: ${sessionInfo.connectedClientsCount} • Latency: ${sessionInfo.latencyMs} ms • Stream: ${sessionInfo.packetsPerSecond} Hz",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSignal
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDisconnect,
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("disconnect_device_button")
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Disconnect")
                    }
                    OutlinedButton(
                        onClick = onReconnect,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reconnect_device_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Reconnect")
                    }
                }
            }
        }

        // QR Pairing & Bluetooth Scan Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (hasBtPermissions) onScanNearby()
                        else btPermLauncher.launch(requiredBtPermissions)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = PitchBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_devices_button")
                ) {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(if (isScanningBluetooth) "Scanning..." else "Scan TV/PC", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { showQrDisplayDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("show_qr_button")
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("TV QR")
                }
                OutlinedButton(
                    onClick = {
                        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        if (hasCam) showQrScannerDialog = true
                        else camPermLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_qr_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Scan")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = bluetoothStatusNote,
                style = MaterialTheme.typography.labelMedium,
                color = CoolSlate
            )
        }

        // Target Device Type Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All") }
                )
                TargetDeviceType.entries.forEach { t ->
                    FilterChip(
                        selected = selectedFilter == t,
                        onClick = { selectedFilter = if (selectedFilter == t) null else t },
                        label = {
                            Text(
                                when (t) {
                                    TargetDeviceType.SMART_TV -> "Smart TV"
                                    TargetDeviceType.LAPTOP_DESKTOP -> "Laptop"
                                    TargetDeviceType.EXTERNAL_DISPLAY -> "Display"
                                }
                            )
                        }
                    )
                }
            }
        }

        // Discovered Devices List
        if (filteredDevices.isEmpty()) {
            item {
                GlassPanel(accentColor = NeonBlue) {
                    Text(
                        text = "No nearby Bluetooth or mDNS receivers detected yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Scan TV/PC' for Bluetooth discovery, open ${sessionInfo.localServerUrl} on any TV/Laptop browser, or enter a PIN below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate
                    )
                }
            }
        } else {
            items(filteredDevices, key = { it.id }) { dev ->
                val isCurrent = sessionInfo.device?.id == dev.id && sessionInfo.status == ConnectionStatus.CONNECTED
                GlassPanel(
                    highlighted = isCurrent,
                    accentColor = if (isCurrent) EmeraldSignal else ElectricCyan,
                    onClick = { onConnectDevice(dev) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_item_${dev.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (dev.type) {
                                    TargetDeviceType.SMART_TV -> Icons.Default.Tv
                                    TargetDeviceType.LAPTOP_DESKTOP -> Icons.Default.Computer
                                    TargetDeviceType.EXTERNAL_DISPLAY -> Icons.Default.DesktopWindows
                                },
                                contentDescription = dev.type.label,
                                tint = ElectricCyan
                            )
                            Column {
                                Text(dev.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(
                                    text = "${dev.source.label} • ${dev.address}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate
                                )
                            }
                        }
                        StatusPill(
                            label = if (isCurrent) "ACTIVE" else "PAIR",
                            color = if (isCurrent) EmeraldSignal else ElectricCyan
                        )
                    }
                }
            }
        }

        // Manual PIN / Host Pairing Card
        item {
            GlassPanel(accentColor = CyberPurple) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manual PIN & Host Pairing",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    TextButton(onClick = onRegeneratePin) {
                        Text("New PIN", color = ElectricCyan)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualPin,
                        onValueChange = { manualPin = it.filter(Char::isDigit).take(6) },
                        label = { Text("4–6 Digit PIN") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_pin_input")
                    )
                    OutlinedTextField(
                        value = manualHost,
                        onValueChange = { manualHost = it },
                        label = { Text("TV / PC IP (optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_host_input")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TargetDeviceType.entries.forEach { t ->
                        FilterChip(
                            selected = manualType == t,
                            onClick = { manualType = t },
                            label = {
                                Text(
                                    when (t) {
                                        TargetDeviceType.SMART_TV -> "Smart TV"
                                        TargetDeviceType.LAPTOP_DESKTOP -> "Laptop"
                                        TargetDeviceType.EXTERNAL_DISPLAY -> "Display"
                                    }
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onConnectManualPin(manualPin, manualHost, manualType) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_pin_connect_button")
                ) {
                    Icon(Icons.Default.CastConnected, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Pair & Open High-Speed Channel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showQrDisplayDialog) {
        val qrPayload = sessionInfo.localServerUrl.ifBlank { "motionx://pair?pin=${sessionInfo.pairingPin}" }
        val qrBitmap = remember(qrPayload) { QrCodeUtils.generateQrBitmap(qrPayload, 512) }
        AlertDialog(
            onDismissRequest = { showQrDisplayDialog = false },
            title = { Text("Pair TV / Laptop Browser") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "MotionX Pairing QR Code",
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    Text(
                        text = "Open on TV / Laptop browser:\n$qrPayload",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = "PIN: ${sessionInfo.pairingPin}",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDisplayDialog = false }) {
                    Text("Close", color = ElectricCyan)
                }
            }
        )
    }

    if (showQrScannerDialog) {
        QrCameraScannerDialog(
            onDismiss = { showQrScannerDialog = false },
            onCodeDetected = { raw ->
                showQrScannerDialog = false
                onQrScanned(raw)
            }
        )
    }
}

@Composable
private fun QrCameraScannerDialog(
    onDismiss: () -> Unit,
    onCodeDetected: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var handled by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan Receiver QR Code") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PitchBlack)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()
                        val scanner = BarcodeScanning.getClient()
                        ProcessCameraProvider.getInstance(ctx).addListener({
                            runCatching {
                                val provider = ProcessCameraProvider.getInstance(ctx).get()
                                val preview = Preview.Builder().build().apply {
                                    surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .apply {
                                        setAnalyzer(cameraExecutor) { imageProxy ->
                                            val mediaImg = imageProxy.image
                                            if (mediaImg != null && !handled) {
                                                val img = InputImage.fromMediaImage(
                                                    mediaImg,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                scanner.process(img)
                                                    .addOnSuccessListener { codes ->
                                                        val raw = codes.firstOrNull()?.rawValue
                                                        if (!raw.isNullOrBlank() && !handled) {
                                                            handled = true
                                                            onCodeDetected(raw)
                                                        }
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                    }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            }
                        }, ContextCompat.getMainExecutor(context))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ElectricCyan)
            }
        }
    )
}
