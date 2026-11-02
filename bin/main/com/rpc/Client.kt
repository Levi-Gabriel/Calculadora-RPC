package com.rpc

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Scanner

// Client Stub: abstrai a chamada remota como métodos locais
class CalculatorRpcClient(private val client: HttpClient, private val endpoint: String) : CalculatorService {
    private suspend fun invokeRemote(method: String, a: Double, b: Double): Double {
        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(RpcRequest(method, a, b))
        }.body<RpcResponse>()

        return response.result ?: throw RuntimeException(response.error ?: "Erro RPC desconhecido")
    }

    override suspend fun somar(a: Double, b: Double): Double = invokeRemote("somar", a, b)
    override suspend fun subtrair(a: Double, b: Double): Double = invokeRemote("subtrair", a, b)
}

fun main() = runBlocking {
    // Configura o HttpClient usando a engine nativa Java do JDK 17
    val httpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json()
        }
        engine {
            // Opções do JavaHttpConfig suportadas pela engine Java
            dispatcher = Dispatchers.IO
        }
    }

    val calculator: CalculatorService = CalculatorRpcClient(httpClient, "http://localhost:8080/rpc")
    val scanner = Scanner(System.`in`)

    println("=== Cliente Calculadora RPC Conectado (Engine Java) ===")
    
    while (true) {
        print("\nDigite a operação (+, - ou 'sair'): ")
        val op = scanner.next()
        if (op.equals("sair", ignoreCase = true)) break

        if (op !in listOf("+", "-")) {
            println("Operação inválida! Use '+' ou '-'.")
            continue
        }

        print("Primeiro número: ")
        val a = scanner.nextDouble()

        print("Segundo número: ")
        val b = scanner.nextDouble()

        try {
            // Chamada RPC tipada via interface
            val resultado = when (op) {
                "+" -> calculator.somar(a, b)
                "-" -> calculator.subtrair(a, b)
                else -> 0.0
            }
            println("-> Resposta remota do servidor: $resultado")
        } catch (e: Exception) {
            println("-> Falha RPC: ${e.message}")
        }
    }

    httpClient.close()
    println("Cliente finalizado.")
}