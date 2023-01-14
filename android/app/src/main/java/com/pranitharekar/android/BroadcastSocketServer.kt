package com.pranitharekar.android

import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*

class BroadcastSocketServer(
    private val broadcastPort: Int = 9997,
    private val broadcastAddress: InetAddress = InetAddress.getByName("255.255.255.255"),
    private val receiveBufferSize: Int = 15000
) {
    private val TAG = "BroadcastSocketServer"
    private var serverIsRunning = true

    fun receive(onReceive: (String) -> Unit) = try {
        // Create socket
        val socket = DatagramSocket(broadcastPort, broadcastAddress)
        socket.broadcast = true

        // Start listening
        while (serverIsRunning) {
            // Start receiving packets
            val receiveBuffer = ByteArray(receiveBufferSize)
            val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(packet)
            val data = String(packet.data).trim { it <= ' ' }
            Log.i(TAG, "Packet received from: " + packet.address.hostAddress + data)
            onReceive(data)
        }

    } catch (ex: IOException) {
        ex.printStackTrace()
    }

    fun send(
        msg: String,
        destinationAddress: InetAddress = broadcastAddress,
    ) =
        try {
            // create socket
            val socket = DatagramSocket()
            socket.broadcast = true

            // Send data
            val sendData = msg.toByteArray()
            val sendPacket = DatagramPacket(
                sendData,
                sendData.size,
                destinationAddress,
                broadcastPort
            )

            // Start sending
            while (serverIsRunning) {
                socket.send(sendPacket)
                Log.i(TAG, "Broadcast packet sent to: " + destinationAddress.hostAddress)
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

    fun stop() {
        serverIsRunning = false
    }
}