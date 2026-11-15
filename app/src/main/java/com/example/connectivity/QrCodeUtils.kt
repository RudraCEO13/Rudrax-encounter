package com.example.connectivity

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.model.DiscoveredDevice
import com.example.model.DiscoverySource
import com.example.model.TargetDeviceType
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeUtils {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return runCatching {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (matrix[x, y]) Color.rgb(4, 6, 10) else Color.rgb(240, 252, 255)
                    )
                }
            }
            bitmap
        }.getOrNull()
    }

    fun parsePairingPayload(raw: String): Pair<DiscoveredDevice, String>? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        return runCatching {
            val uri = Uri.parse(trimmed)
            val host = uri.getQueryParameter("host") ?: uri.host ?: return null
            val port = uri.getQueryParameter("port")?.toIntOrNull() ?: if (uri.port > 0) uri.port else 8765
            val pin = uri.getQueryParameter("pin").orEmpty()
            val name = uri.getQueryParameter("name") ?: "QR Receiver ($host)"
            val typeStr = uri.getQueryParameter("type")
            val type = runCatching {
                if (typeStr != null) TargetDeviceType.valueOf(typeStr) else TargetDeviceType.SMART_TV
            }.getOrDefault(TargetDeviceType.SMART_TV)

            DiscoveredDevice(
                id = "qr_${host}_$port",
                name = name,
                address = host,
                port = port,
                type = type,
                source = DiscoverySource.QR_CODE,
                supportsWebRtc = true,
                isPaired = false
            ) to pin
        }.getOrNull()
    }
}
