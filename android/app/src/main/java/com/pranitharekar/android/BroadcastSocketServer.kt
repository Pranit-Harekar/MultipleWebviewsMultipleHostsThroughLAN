package com.pranitharekar.android

import android.content.Context
import android.net.wifi.WifiManager
import android.os.StrictMode
import android.text.format.Formatter
import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


// range of 224.0.0.0 to 239.255.255.255 are reserved for multicasting
class BroadcastSocketServer(private val context: Context) {
    val TAG = "BroadcastSocketServer"
    val PORT = 9997
    val INET_ADDR = "255.255.255.255"
    val addr: InetAddress = InetAddress.getByName(INET_ADDR)

    fun listen() {

        // Create a buffer of bytes, which will be used to store
        // the incoming bytes containing the information from the server.
        // Since the message is small here, 256 bytes should be enough.
        // Create a buffer of bytes, which will be used to store
        // the incoming bytes containing the information from the server.
        // Since the message is small here, 256 bytes should be enough.
        val buf = ByteArray(256)

        // Create a new Multicast socket (that will allow other sockets/programs
        // to join it as well.
        try {
            val socket = DatagramSocket(PORT, addr)
            socket.broadcast = true

            while (true) {
                Log.i(TAG, "Ready to receive broadcast packets!")
                val recvBuf = ByteArray(15000)
                val packet = DatagramPacket(recvBuf, recvBuf.size)
                socket.receive(packet)
                Log.i(TAG, "Packet received from: " + packet.address.hostAddress)
                val data = String(packet.data).trim { it <= ' ' }
                Log.i(TAG, "Packet received; data: $data")

                if (data.lowercase() == "who has cfd") {
                    val msg = "CFD is at ${getIPAddress()}"
                    send(msg, getBroadcastAddress())
                }
            }

        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun getIPAddress(): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }

    @Throws(IOException::class)
    fun getBroadcastAddress(): InetAddress {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }

    private fun send(msg: String, destAddr: InetAddress) {
        Log.i(TAG, "Ready to send broadcast packets; Data: $msg")
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val sendData = msg.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, destAddr, PORT)
            socket.send(sendPacket)
            Log.d(TAG, "Broadcast packet sent to: " + destAddr.hostAddress)
        } catch (e: IOException) {
            Log.e(TAG, "IOException: " + e.message)
        }
    }
}