package com.rpc // Pacote do projeto

import io.ktor.client.* // Classe HttpClient
import io.ktor.client.call.* // Função body<T>() para ler a resposta
import io.ktor.client.engine.java.* // Engine HTTP nativa do JDK
import io.ktor.client.plugins.contentnegotiation.* // Plugin de conversão JSON <-> objetos
import io.ktor.client.request.* // Funções post e setBody
import io.ktor.http.* // ContentType e demais tipos HTTP
import io.ktor.serialization.kotlinx.json.* // Suporte a JSON via kotlinx.serialization
import kotlinx.coroutines.Dispatchers // Dispatchers de corrotinas
import kotlinx.coroutines.runBlocking // Executa código suspend na função main

// Client Stub: abstrai a chamada remota como métodos locais
class CalculatorRpcClient(private val client: HttpClient, private val endpoint: String) : CalculatorService {

    private suspend fun invokeRemote(request: RpcRequest): Double { // Envia a requisição RPC e devolve o resultado
        val response = client.post(endpoint) { // Faz um POST HTTP ao endpoint do servidor
            contentType(ContentType.Application.Json) // Informa que o corpo é JSON
            setBody(request) // Serializa a requisição RPC no corpo
        }.body<RpcResponse>() // Lê e desserializa a resposta JSON

        return response.result ?: throw RuntimeException(response.error ?: "Erro RPC desconhecido") // Devolve o resultado ou lança o erro do servidor
    }

    override suspend fun somar(a: Double, b: Double): Double = invokeRemote(RpcRequest("somar", a, b)) // Chamada remota de soma
    override suspend fun subtrair(a: Double, b: Double): Double = invokeRemote(RpcRequest("subtrair", a, b)) // Chamada remota de subtração
    override suspend fun multiplicar(a: Double, b: Double): Double = invokeRemote(RpcRequest("multiplicar", a, b)) // Chamada remota de multiplicação
    override suspend fun dividir(a: Double, b: Double): Double = invokeRemote(RpcRequest("dividir", a, b)) // Chamada remota de divisão
    override suspend fun avaliarExpressao(expressao: String): Double = invokeRemote(RpcRequest("avaliarExpressao", expression = expressao)) // Envia a expressão inteira
}

fun main() = runBlocking { // Ponto de entrada do cliente
    // Configura o HttpClient usando a engine nativa Java do JDK 17
    val httpClient = HttpClient(Java) { // Cria o cliente HTTP com a engine Java
        install(ContentNegotiation) { // Instala a conversão automática de JSON
            json() // Usa kotlinx.serialization para JSON
        }
        engine { // Opções da engine Java
            dispatcher = Dispatchers.IO // Usa o dispatcher de I/O para as requisições
        }
    }

    val calculator: CalculatorService = CalculatorRpcClient(httpClient, "http://localhost:8080/rpc") // Stub apontando para o servidor

    while (true) { // Laço do menu principal
        println("\n=== Calculadora JSON-RPC ===") // Título do menu
        println("1 - Operação simples (ex.: 10 + 15)") // Opção 1
        println("2 - Expressão calculada no CLIENTE (várias chamadas remotas)") // Opção 2 (abordagem 1)
        println("3 - Expressão calculada no SERVIDOR (uma chamada remota)") // Opção 3 (abordagem 2)
        println("0 - Sair") // Opção de saída
        print("Escolha: ") // Solicita a opção
        val op = readLine()?.trim() ?: break // Lê a opção (encerra se a entrada acabar)

        if (op == "0") break // Sai do laço

        try { // Trata erros de rede, de cálculo e de formato
            when (op) {
                "1" -> { // Operação simples
                    print("Digite (ex.: 10 + 15): ") // Solicita a operação
                    val p = (readLine() ?: "").trim().split(Regex("\\s+")) // Separa operando, operador, operando
                    if (p.size != 3) { // Valida o formato
                        println("Formato inválido. Use: número operador número") // Mensagem de erro
                        continue // Volta ao menu
                    }
                    val a = p[0].toDouble() // Converte o primeiro operando
                    val b = p[2].toDouble() // Converte o segundo operando
                    val r = when (p[1]) { // Escolhe a chamada remota conforme o operador
                        "+" -> calculator.somar(a, b) // Soma remota
                        "-" -> calculator.subtrair(a, b) // Subtração remota
                        "*" -> calculator.multiplicar(a, b) // Multiplicação remota
                        "/" -> calculator.dividir(a, b) // Divisão remota
                        else -> { println("Operador inválido."); continue } // Operador desconhecido
                    }
                    println("-> Resposta remota do servidor: $r") // Exibe o resultado
                }
                "2", "3" -> { // Expressões com parênteses
                    print("Digite a expressão (ex.: (10 + 15) * 4): ") // Solicita a expressão
                    val expr = readLine() ?: "" // Lê a expressão
                    val r = if (op == "2") { // Abordagem 1: interpretação no cliente
                        Evaluator(expr, calculator).avalia() // Avaliador local com o STUB: cada operação vira uma chamada remota
                    } else { // Abordagem 2: cálculo no servidor
                        calculator.avaliarExpressao(expr) // Envia a expressão inteira e recebe só o resultado
                    }
                    println("-> Resposta remota do servidor: $r") // Exibe o resultado
                }
                else -> println("Opção inválida.") // Opção inexistente
            }
        } catch (e: Exception) { // Qualquer falha na operação
            println("-> Falha RPC: ${e.message}") // Informa o erro
        }
    }

    httpClient.close() // Libera os recursos do cliente HTTP
    println("Cliente finalizado.") // Mensagem de encerramento
}