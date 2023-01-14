package com.pranitharekar.android

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.pranitharekar.android.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var webview: WebView
    private val browserPlugin = "browserPlugin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        webview = binding.webview
        setupWebviewWrapper()

        // UDP broadcast
        val udpServer = BroadcastSocketServer()
        CoroutineScope(Dispatchers.IO).launch {
            val hostAddress = getHostAddress() // address of the android host

            udpServer.receive() { message ->
                if (message.lowercase() == "who has cfd") {
                    val msg = "CFD is at $hostAddress"
                    udpServer.send(msg)
                }
            }
        }

        // HTTP Server
        CoroutineScope(Dispatchers.IO).launch {
            val hostAddress = getHostAddress()
            val hostPort = 8080
            val httpServer = HTTPServer()

            httpServer.start(hostAddress, hostPort) {
                udpServer.stop()
                runOnUiThread {
                    webview.visibility = WebView.VISIBLE
                    val js = "var bc2 = new BroadcastChannel(\"app_2\"); bc2.postMessage(\"$it\");"
                    webview.post {
                        webview.evaluateJavascript(js, null)
                    }
                }
            }
        }
    }

    // Helpers
    @Suppress("DEPRECATION")
    private fun getHostAddress(): String {
        val wifiManager =
            this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }

    private inner class JavaScriptInterface {
        @JavascriptInterface
        fun postMessage(message: String) {
            Log.d("JavascriptInterface", "JavaScriptInterface postMessage received: $message")
        }
    }

    private fun setupWebviewWrapper() {
        val webSettings: WebSettings = webview.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true

        webview.addJavascriptInterface(JavaScriptInterface(), browserPlugin)
        webview.loadUrl("file:///android_asset/app2.html")
    }
}