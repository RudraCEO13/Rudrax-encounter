package com.example.model

enum class TargetDeviceType(val label: String) {
    SMART_TV("Android / Google Smart TV"),
    LAPTOP_DESKTOP("Laptop / Desktop"),
    EXTERNAL_DISPLAY("External Display")
}

enum class DiscoverySource(val label: String) {
    BLUETOOTH("Bluetooth Pairing"),
    WIFI_NSD("Local Wi-Fi mDNS"),
    QR_CODE("QR Handshake"),
    MANUAL_PIN("PIN / Direct Host"),
    HDMI_CAST("External Display API")
}

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    DISCOVERING("Scanning Nearby"),
    PAIRING("Verifying PIN"),
    CONNECTED("Connected • Low Latency"),
    RECONNECTING("Reconnecting"),
    ERROR("Connection Error")
}

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val address: String,
    val port: Int = 8765,
    val type: TargetDeviceType,
    val source: DiscoverySource,
    val signalStrengthDbm: Int = -55,
    val supportsWebRtc: Boolean = true,
    val isPaired: Boolean = false
)

data class ConnectedSessionInfo(
    val device: DiscoveredDevice? = null,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val pairingPin: String = "",
    val transportChannel: String = "Local Wi-Fi WebSocket + WebRTC Signaling",
    val localServerUrl: String = "",
    val wsStreamUrl: String = "",
    val connectedClientsCount: Int = 0,
    val latencyMs: Int = 0,
    val packetsPerSecond: Int = 0,
    val lastError: String? = null,
    val isDisplayMirroredToTv: Boolean = false
)
