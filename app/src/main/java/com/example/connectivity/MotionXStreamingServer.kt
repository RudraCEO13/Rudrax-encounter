package com.example.connectivity

import android.util.Log
import com.example.runtime.GameStorageManager
import com.example.runtime.MotionXSdkTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class MotionXStreamingServer(
    private val storageManager: GameStorageManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var httpSocket: ServerSocket? = null
    private var wsServer: InternalWsServer? = null
    private val isRunning = AtomicBoolean(false)

    private var currentPin: String = "482910"
    private var activeGameId: String? = null
    private var activeEntryPoint: String = "index.html"
    private var currentHttpPort: Int = 8088
    private var currentWsPort: Int = 8765

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    private val _serverReady = MutableStateFlow(false)
    val serverReady: StateFlow<Boolean> = _serverReady.asStateFlow()

    fun start(httpPort: Int = 8088, wsPort: Int = 8765, pin: String) {
        currentPin = pin
        if (isRunning.get()) return
        isRunning.set(true)

        scope.launch {
            startHttpServer(httpPort)
            startWsServer(wsPort)
            _serverReady.value = true
        }
    }

    fun updatePairingPin(pin: String) {
        currentPin = pin
    }

    fun setActiveGame(gameId: String, entryPoint: String) {
        activeGameId = gameId
        activeEntryPoint = entryPoint
        val loadMsg = JSONObject().apply {
            put("type", "load_game")
            put("gameId", gameId)
            put("entryPoint", entryPoint)
        }.toString()
        wsServer?.broadcastAuthenticated(loadMsg)
    }

    fun broadcastChannelEvent(channel: String, payloadJson: String) {
        val msg = """{"channel":"$channel","payload":$payloadJson}"""
        wsServer?.broadcastAuthenticated(msg)
    }

    fun getLocalIpAddress(): String {
        return runCatching {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }

    fun getLoopbackGameUrl(gameId: String, entryPoint: String): String {
        val cleanEntry = entryPoint.trimStart('/')
        return "http://127.0.0.1:$currentHttpPort/games/$gameId/$cleanEntry"
    }

    fun getLanReceiverUrl(pin: String): String {
        return "http://${getLocalIpAddress()}:$currentHttpPort/?pin=$pin"
    }

    fun getWsStreamUrl(): String = "ws://${getLocalIpAddress()}:$currentWsPort"

    fun getHttpPort(): Int = currentHttpPort
    fun getWsPort(): Int = currentWsPort

    private fun startHttpServer(preferredPort: Int) {
        var boundSocket: ServerSocket? = null
        for (candidate in listOf(preferredPort, preferredPort + 1, preferredPort + 2, 0)) {
            val s = runCatching { ServerSocket(candidate) }.getOrNull()
            if (s != null) {
                boundSocket = s
                currentHttpPort = s.localPort
                break
            }
        }
        httpSocket = boundSocket
        val server = boundSocket ?: return

        scope.launch {
            while (isRunning.get() && !server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                scope.launch { handleHttpClient(client) }
            }
        }
    }

    private fun handleHttpClient(socket: Socket) {
        socket.use { sock ->
            runCatching {
                val input = BufferedInputStream(sock.getInputStream())
                val requestLine = readLine(input) ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val rawPath = parts[1].substringBefore('?')

                // Drain headers
                while (true) {
                    val line = readLine(input)
                    if (line.isNullOrEmpty()) break
                }

                val out = sock.getOutputStream()
                when {
                    rawPath == "/" || rawPath == "/receiver.html" -> {
                        val html = MotionXSdkTemplates.buildReceiverHtml(currentWsPort, activeGameId)
                        writeHttpResponse(out, "200 OK", "text/html; charset=utf-8", html.toByteArray())
                    }
                    rawPath == "/sdk/motionx-sdk.js" -> {
                        writeHttpResponse(
                            out,
                            "200 OK",
                            "application/javascript; charset=utf-8",
                            MotionXSdkTemplates.MOTIONX_SDK_JS.toByteArray()
                        )
                    }
                    rawPath.startsWith("/games/") -> {
                        val remainder = rawPath.removePrefix("/games/")
                        val slashIdx = remainder.indexOf('/')
                        if (slashIdx <= 0) {
                            writeHttpResponse(out, "404 Not Found", "text/plain", "Game not found".toByteArray())
                            return
                        }
                        val gameId = remainder.substring(0, slashIdx)
                        val relPath = remainder.substring(slashIdx + 1).ifBlank { "index.html" }
                        val file = storageManager.resolveGameFile(gameId, relPath)
                        if (file == null) {
                            writeHttpResponse(out, "404 Not Found", "text/plain", "Asset not found".toByteArray())
                        } else {
                            val mime = guessMimeType(file.extension)
                            writeHttpResponse(out, "200 OK", mime, file.readBytes())
                        }
                    }
                    else -> {
                        writeHttpResponse(out, "404 Not Found", "text/plain", "Not Found".toByteArray())
                    }
                }
            }
        }
    }

    private fun writeHttpResponse(
        out: java.io.OutputStream,
        status: String,
        contentType: String,
        body: ByteArray
    ) {
        val headers = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(headers.toByteArray())
        out.write(body)
        out.flush()
    }

    private fun readLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) return if (baos.size() == 0) null else baos.toString("UTF-8")
            if (b == '\n'.code) break
            if (b != '\r'.code) baos.write(b)
        }
        return baos.toString("UTF-8")
    }

    private fun startWsServer(preferredPort: Int) {
        runCatching {
            currentWsPort = preferredPort
            wsServer = InternalWsServer(InetSocketAddress(preferredPort)).apply {
                isReuseAddr = true
                start()
            }
        }.onFailure { err ->
            Log.e(TAG, "WebSocket server bind failed on $preferredPort", err)
        }
    }

    fun stop() {
        isRunning.set(false)
        _serverReady.value = false
        runCatching { httpSocket?.close() }
        runCatching { wsServer?.stop(500) }
        httpSocket = null
        wsServer = null
        _connectedClients.value = 0
    }

    private inner class InternalWsServer(address: InetSocketAddress) : WebSocketServer(address) {
        private val authenticatedSockets = ConcurrentHashMap.newKeySet<WebSocket>()

        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            // Await PIN auth message
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            if (conn != null) authenticatedSockets.remove(conn)
            _connectedClients.value = authenticatedSockets.size
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            if (conn == null || message.isNullOrBlank()) return
            runCatching {
                val json = JSONObject(message)
                when (json.optString("type")) {
                    "auth" -> {
                        val pin = json.optString("pin")
                        if (pin == currentPin || currentPin.isBlank()) {
                            authenticatedSockets.add(conn)
                            _connectedClients.value = authenticatedSockets.size
                            conn.send(JSONObject().apply {
                                put("type", "auth_ok")
                                put("activeGameId", activeGameId ?: "")
                                put("entryPoint", activeEntryPoint)
                            }.toString())
                        } else {
                            conn.send("""{"type":"auth_error"}""")
                        }
                    }
                    "rtc_offer", "rtc_answer", "rtc_ice" -> {
                        // Relay WebRTC DataChannel signaling between paired peers
                        authenticatedSockets.filter { it != conn }.forEach { peer ->
                            runCatching { peer.send(message) }
                        }
                    }
                }
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            Log.w(TAG, "WS error: ${ex?.message}")
        }

        override fun onStart() {
            Log.i(TAG, "MotionX WebSocket streaming server listening on $port")
        }

        fun broadcastAuthenticated(payload: String) {
            if (authenticatedSockets.isEmpty()) return
            authenticatedSockets.forEach { sock ->
                if (sock.isOpen) {
                    runCatching { sock.send(payload) }
                }
            }
        }
    }

    private fun guessMimeType(ext: String): String = when (ext.lowercase()) {
        "html", "htm" -> "text/html; charset=utf-8"
        "js", "mjs" -> "application/javascript; charset=utf-8"
        "css" -> "text/css; charset=utf-8"
        "json", "gltf" -> "application/json; charset=utf-8"
        "glb" -> "model/gltf-binary"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "svg" -> "image/svg+xml"
        "webp" -> "image/webp"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        else -> "application/octet-stream"
    }

    companion object {
        private const val TAG = "MotionXStreamingServer"
    }
}
