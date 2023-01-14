package com.pranitharekar.android

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import com.pranitharekar.android.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    binding.textviewFirst.text = it
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
}