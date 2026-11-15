package com.example.runtime

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

class MotionXWebBridge(
    private val onSdkFeedback: (String) -> Unit
) {
    @JavascriptInterface
    fun onGameFeedback(payload: String) {
        onSdkFeedback(payload)
    }

    @JavascriptInterface
    fun onConfigUpdate(configJson: String) {
        onSdkFeedback("Config updated: $configJson")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MotionXRuntimeWebView(
    gameUrl: String,
    isRunning: Boolean,
    latestChannelMessageJson: Pair<String, String>?,
    onSdkFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            addJavascriptInterface(MotionXWebBridge(onSdkFeedback), "MotionXAndroidBridge")
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val host = request?.url?.host.orEmpty()
                    // Sandbox: allow loopback runtime and reject arbitrary external navigations
                    return host != "127.0.0.1" && host != "localhost"
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Inject MotionX SDK into any uploaded HTML game automatically
                    view?.evaluateJavascript(MotionXSdkTemplates.MOTIONX_SDK_JS, null)
                }
            }
        }
    }

    LaunchedEffect(gameUrl, isRunning) {
        if (isRunning && gameUrl.isNotBlank()) {
            webView.loadUrl(gameUrl)
        } else if (!isRunning) {
            webView.loadUrl("about:blank")
        }
    }

    LaunchedEffect(latestChannelMessageJson) {
        val msg = latestChannelMessageJson ?: return@LaunchedEffect
        if (isRunning) {
            val channel = msg.first
            val json = msg.second
            webView.evaluateJavascript(
                "if (window.MotionX && window.MotionX.__dispatch) { window.MotionX.__dispatch('$channel', $json); }",
                null
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
