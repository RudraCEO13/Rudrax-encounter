package com.example.connectivity

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.model.DiscoveredDevice
import com.example.model.DiscoverySource
import com.example.model.TargetDeviceType
import com.example.runtime.MotionXSdkTemplates

class ExternalGamePresentation(
    outerContext: Context,
    display: Display,
    private val gameUrl: String
) : Presentation(outerContext, display) {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wv = WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(MotionXSdkTemplates.MOTIONX_SDK_JS, null)
                }
            }
            loadUrl(gameUrl)
        }
        webView = wv
        setContentView(wv)
    }

    fun dispatchMotion(channel: String, jsonPayload: String) {
        webView?.post {
            webView?.evaluateJavascript(
                "if (window.MotionX && window.MotionX.__dispatch) { window.MotionX.__dispatch('$channel', $jsonPayload); }",
                null
            )
        }
    }

    override fun onStop() {
        webView?.destroy()
        webView = null
        super.onStop()
    }
}

class ExternalDisplayController(private val context: Context) {
    private val displayManager: DisplayManager? by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    }

    private var activePresentation: ExternalGamePresentation? = null

    fun listExternalDisplays(): List<DiscoveredDevice> {
        val displays = displayManager?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            ?: emptyArray()
        return displays.map { display ->
            DiscoveredDevice(
                id = "display_${display.displayId}",
                name = display.name ?: "External Display #${display.displayId}",
                address = "DisplayID:${display.displayId}",
                port = 0,
                type = TargetDeviceType.EXTERNAL_DISPLAY,
                source = DiscoverySource.HDMI_CAST,
                signalStrengthDbm = -20,
                supportsWebRtc = false,
                isPaired = true
            )
        }
    }

    fun showGameOnDisplay(displayId: Int, gameUrl: String): Boolean {
        val display = displayManager?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            ?.firstOrNull { it.displayId == displayId } ?: return false
        dismissPresentation()
        return runCatching {
            activePresentation = ExternalGamePresentation(context, display, gameUrl).apply {
                show()
            }
            true
        }.getOrDefault(false)
    }

    fun dispatchMotion(channel: String, jsonPayload: String) {
        activePresentation?.dispatchMotion(channel, jsonPayload)
    }

    fun dismissPresentation() {
        runCatching { activePresentation?.dismiss() }
        activePresentation = null
    }
}
