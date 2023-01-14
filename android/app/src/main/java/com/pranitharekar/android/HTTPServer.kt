package com.pranitharekar.android

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*

class HTTPServer() {
    private lateinit var server: NettyApplicationEngine

    fun start(host: String, port: Int, log: (String) -> Unit) {
        server = embeddedServer(Netty, port, host) {
            routing {
                get("/cfd") {
                    val response = "Hello, world!"
                    call.respondText(response)
                    log("received GET '/' request")
                }

                post("/cfd") {
                    val response = "Hello CFD!"
                    call.respondText(response)
                    val requestBody = call.receiveParameters()
                    val data = requestBody["data"]
                    log("received POST '/cfd' request with '$data'")
                }
            }
        }
        server.start(wait = true)
    }

    fun stop() {
        server.stop()
    }
}