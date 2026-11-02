package com.rpc

import io.ktor.serialization.kotlinx.json.* // Suporte a JSON via kotlinx.serialization
import io.ktor.server.application.* // Classes base da aplicação Ktor
import io.ktor.server.engine.* // Permite criar o servidor embutido
import io.ktor.server.netty.* // Engine Netty (servidor HTTP)
import io.ktor.server.plugins.contentnegotiation.* // Plugin que converte corpo HTTP <-> objetos
import io.ktor.server.request.* // Funções para ler a requisição (receive)
import io.ktor.server.response.* // Funções para enviar a resposta (respond)
import io.ktor.server.routing.* // DSL de rotas (routing, post)

// Implementação do serviço: contém a lógica de cálculo executada no servidor (sem log)
class CalculatorServiceImpl : CalculatorService {
    override suspend fun somar(a: Double, b: Double): Double = a + b // Soma

    override suspend fun subtrair(a: Double, b: Double): Double = a - b // Subtração

    override suspend fun multiplicar(a: Double, b: Double): Double = a * b // Multiplicação

    override suspend fun dividir(a: Double, b: Double): Double { // Divisão
        if (b == 0.0) throw ArithmeticException("Divisão por zero") // Protege contra divisão por zero
        return a / b
    }

    // Abordagem 2: avalia a expressão inteira aqui, usando esta própria classe (sem chamadas de rede)
    override suspend fun avaliarExpressao(expressao: String): Double = Evaluator(expressao, this).avalia()
}

fun main() { // Ponto de entrada do servidor
    val service: CalculatorService = CalculatorServiceImpl() // Cria a implementação do serviço

    embeddedServer(Netty, port = 8080) { // Sobe um servidor Netty na porta desejada
        install(ContentNegotiation) { // Instala a conversão automática de JSON
            json() // Usa kotlinx.serialization para JSON
        }

        routing { // Define as rotas HTTP
            post("/rpc") { // Endpoint único que recebe todas as chamadas RPC
                val req = call.receive<RpcRequest>() // Lê e desserializa o JSON da requisição

                // Log de cada chamada que chegou pela rede
                println("[Servidor] RPC recebido: ${req.method} a=${req.a} b=${req.b} expressao=${req.expression}")

                val resposta = try {
                    val resultado = when (req.method) { // Escolhe o método remoto pelo nome

                        "somar" -> service.somar(req.a, req.b)
                        "subtrair" -> service.subtrair(req.a, req.b)
                        "multiplicar" -> service.multiplicar(req.a, req.b)
                        "dividir" -> service.dividir(req.a, req.b)
                        "avaliarExpressao" -> service.avaliarExpressao(
                            req.expression ?: throw IllegalArgumentException("Expressão ausente") // Exige o campo expression
                        )
                        else -> throw IllegalArgumentException("Método '${req.method}' desconhecido") // Método inexistente
                    }
                    RpcResponse(result = resultado) // Resposta de sucesso com o resultado
                } catch (e: Exception) {
                    RpcResponse(error = e.message ?: "Erro interno") // Resposta de erro com a mensagem
                }

                call.respond(resposta) // Envia a resposta JSON ao cliente
            }
        }
    }.start(wait = true) // Inicia o servidor e bloqueia a thread principal
}