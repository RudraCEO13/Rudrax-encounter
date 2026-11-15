package com.example.connectivity

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.example.model.DiscoveredDevice
import com.example.model.DiscoverySource
import com.example.model.TargetDeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NsdDiscoveryManager(private val context: Context) {

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private val _wifiDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val wifiDevices: StateFlow<List<DiscoveredDevice>> = _wifiDevices.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun registerControllerService(httpPort: Int) {
        val mgr = nsdManager ?: return
        unregisterService()
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "RudraX-MotionX-${Build.MODEL.take(12)}"
            serviceType = SERVICE_TYPE
            port = httpPort
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        runCatching {
            mgr.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    fun startWifiDiscovery() {
        val mgr = nsdManager ?: return
        stopWifiDiscovery()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                mgr.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        val port = resolved.port
                        val name = resolved.serviceName ?: "MotionX Receiver"
                        val type = if (name.contains("Laptop", true) || name.contains("PC", true)) {
                            TargetDeviceType.LAPTOP_DESKTOP
                        } else {
                            TargetDeviceType.SMART_TV
                        }
                        val device = DiscoveredDevice(
                            id = "nsd_${host}_$port",
                            name = name,
                            address = host,
                            port = port,
                            type = type,
                            source = DiscoverySource.WIFI_NSD,
                            signalStrengthDbm = -42,
                            supportsWebRtc = true
                        )
                        val list = _wifiDevices.value.toMutableList()
                        val idx = list.indexOfFirst { it.id == device.id }
                        if (idx >= 0) list[idx] = device else list.add(device)
                        _wifiDevices.value = list
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        discoveryListener = listener
        runCatching {
            mgr.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    fun stopWifiDiscovery() {
        val mgr = nsdManager ?: return
        discoveryListener?.let { listener ->
            runCatching { mgr.stopServiceDiscovery(listener) }
        }
        discoveryListener = null
    }

    fun unregisterService() {
        val mgr = nsdManager ?: return
        registrationListener?.let { listener ->
            runCatching { mgr.unregisterService(listener) }
        }
        registrationListener = null
    }

    companion object {
        private const val SERVICE_TYPE = "_motionx._tcp."
    }
}
