package com.example.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.DiscoveredDevice
import com.example.model.DiscoverySource
import com.example.model.TargetDeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothDiscoveryManager(private val context: Context) {

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusNote = MutableStateFlow("Ready to scan Bluetooth")
    val statusNote: StateFlow<String> = _statusNote.asStateFlow()

    private var receiverRegistered = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        mgr?.adapter
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (-60).toShort()).toInt()
                    device?.let { addOrUpdateDevice(it, rssi) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    _statusNote.value = "Scan complete (${_devices.value.size} found)"
                }
            }
        }
    }

    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _statusNote.value = "Bluetooth radio unavailable on this device (use Wi-Fi / QR / PIN)"
            _isScanning.value = false
            return
        }
        if (!hasPermissions()) {
            _statusNote.value = "Bluetooth permission required for nearby device discovery"
            _isScanning.value = false
            return
        }
        if (!adapter.isEnabled) {
            _statusNote.value = "Bluetooth is turned off"
            _isScanning.value = false
            return
        }

        // Load already-bonded compatible devices immediately
        runCatching {
            adapter.bondedDevices?.forEach { bonded ->
                addOrUpdateDevice(bonded, -48, paired = true)
            }
        }

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            ContextCompat.registerReceiver(
                context,
                discoveryReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }

        runCatching {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            val started = adapter.startDiscovery()
            _isScanning.value = started
            _statusNote.value = if (started) {
                "Scanning Bluetooth TVs & Laptops..."
            } else {
                "Loaded paired Bluetooth devices"
            }
        }.onFailure {
            _isScanning.value = false
            _statusNote.value = "Discovery error: ${it.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (hasPermissions()) {
            runCatching {
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            }
        }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDevice(device: BluetoothDevice, rssi: Int, paired: Boolean = false) {
        val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: return
        val address = device.address ?: return
        val majorClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
        val type = when {
            majorClass == BluetoothClass.Device.Major.COMPUTER ||
                name.contains("Mac", ignoreCase = true) ||
                name.contains("PC", ignoreCase = true) ||
                name.contains("Laptop", ignoreCase = true) -> TargetDeviceType.LAPTOP_DESKTOP
            name.contains("TV", ignoreCase = true) ||
                name.contains("Bravia", ignoreCase = true) ||
                name.contains("Chromecast", ignoreCase = true) ||
                majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO -> TargetDeviceType.SMART_TV
            else -> TargetDeviceType.EXTERNAL_DISPLAY
        }
        val item = DiscoveredDevice(
            id = "bt_$address",
            name = name,
            address = address,
            port = 8765,
            type = type,
            source = DiscoverySource.BLUETOOTH,
            signalStrengthDbm = rssi,
            supportsWebRtc = true,
            isPaired = paired || (runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false))
        )
        val current = _devices.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) current[idx] = item else current.add(item)
        _devices.value = current
    }

    fun release() {
        stopDiscovery()
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(discoveryReceiver) }
            receiverRegistered = false
        }
    }
}
