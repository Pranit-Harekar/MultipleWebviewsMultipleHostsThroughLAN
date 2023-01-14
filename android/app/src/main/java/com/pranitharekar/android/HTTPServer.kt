package com.pranitharekar.android

import android.util.Log
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*

class HTTPServer() {
    private lateinit var server: NettyApplicationEngine

    fun start(host: String, port: Int, onReceive: (String) -> Unit) {
        server = embeddedServer(Netty, port, host) {
            routing {
                get("/cfd") {
                    val response = "Hello, world!"
                    call.respondText(response)
                    Log.d("HTTPSERVER", "received GET '/' request")
                    onReceive("")
                }

                post("/cfd") {
                    val response = "Hello CFD!"
                    call.respondText(response)
                    val requestBody = call.receiveParameters()
                    val data = requestBody["data"]
                    Log.d("HTTPSERVER", "received POST '/cfd' request with '$data'")
                    onReceive("$data")
                }
            }
        }
        server.start(wait = true)
    }

    fun stop() {
        server.stop()
    }
}