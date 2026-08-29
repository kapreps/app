package com.kapreps.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * Parent portal APK for https://app.kapreps.com
 * Package / applicationId: com.kapreps.app (reverse of app.kapreps.com).
 */
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setSupportZoom(false)
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportMultipleWindows(true)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val popup = WebView(this@MainActivity)
                popup.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val uri = request?.url ?: return true
                        openExternal(uri)
                        return true
                    }
                }
                transport.webView = popup
                resultMsg.sendToTarget()
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val uri = request?.url ?: return false
                if (shouldOpenExternally(uri)) {
                    openExternal(uri)
                    return true
                }
                return false
            }
        }
        webView.loadUrl(PORTAL_START_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun shouldOpenExternally(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == "tel" || scheme == "mailto" || scheme == "sms" || scheme == "whatsapp") {
            return true
        }
        val host = uri.host?.lowercase()?.removePrefix("www.").orEmpty()
        if (host == "wa.me" || host == "api.whatsapp.com" || host.endsWith(".whatsapp.com")) {
            return true
        }
        if (scheme == "http" || scheme == "https") {
            return !isPortalHost(uri.host)
        }
        return false
    }

    private fun openExternal(uri: Uri) {
        val scheme = uri.scheme?.lowercase()
        val intent = when (scheme) {
            "tel" -> Intent(Intent.ACTION_DIAL, uri)
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            else -> Intent(Intent.ACTION_VIEW, uri)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No matching app — stay in the portal.
        }
    }

    companion object {
        // Canonical production site from mentoreship URL defaults.
        const val PORTAL_ORIGIN = "https://app.kapreps.com"
        const val PORTAL_START_URL = "$PORTAL_ORIGIN/parent"

        private fun isPortalHost(host: String?): Boolean {
            val normalized = host?.lowercase()?.removePrefix("www.") ?: return false
            return normalized == "app.kapreps.com"
        }
    }
}
