package com.rpc

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class CalculatorServiceImpl : CalculatorService {
    override suspend fun somar(a: Double, b: Double): Double {
        println("[Servidor] Calculando soma: $a + $b")
        return a + b
    }

    override suspend fun subtrair(a: Double, b: Double): Double {
        println("[Servidor] Calculando subtração: $a - $b")
        return a - b
    }
}

fun main() {
    val service: CalculatorService = CalculatorServiceImpl()

    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            post("/rpc") {
                val req = call.receive<RpcRequest>()
                val resultado = when (req.method) {
                    "somar" -> service.somar(req.a, req.b)
                    "subtrair" -> service.subtrair(req.a, req.b)
                    else -> null
                }

                if (resultado != null) {
                    call.respond(RpcResponse(result = resultado))
                } else {
                    call.respond(RpcResponse(error = "Método '${req.method}' desconhecido"))
                }
            }
        }
    }.start(wait = true)
}