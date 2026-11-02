package com.rpc // Pacote do projeto

import kotlinx.serialization.Serializable // Anotação que gera o serializador JSON da classe

// Payload no padrão JSON-RPC para transportar as chamadas
@Serializable // Permite converter o objeto de/para JSON
data class RpcRequest( // Requisição enviada do cliente ao servidor
    val method: String, // Nome do método remoto a ser invocado (ex.: "somar", "avaliarExpressao")
    val a: Double = 0.0, // Primeiro operando (usado nas operações básicas)
    val b: Double = 0.0, // Segundo operando (usado nas operações básicas)
    val expression: String? = null // Expressão completa (usada só em "avaliarExpressao")
)

@Serializable // Permite converter o objeto de/para JSON
data class RpcResponse( // Resposta enviada do servidor ao cliente
    val result: Double? = null, // Resultado do cálculo quando a chamada tem sucesso
    val error: String? = null // Mensagem de erro quando a chamada falha
)

// Contrato de RPC da Calculadora
interface CalculatorService { // Interface compartilhada por cliente (stub) e servidor (implementação)
    suspend fun somar(a: Double, b: Double): Double // Soma a + b
    suspend fun subtrair(a: Double, b: Double): Double // Subtração a - b
    suspend fun multiplicar(a: Double, b: Double): Double // Multiplicação a * b
    suspend fun dividir(a: Double, b: Double): Double // Divisão a / b (erro se b == 0)

    // Abordagem 2: o servidor recebe a expressão inteira (ex.: "(10 + 15) * 4") e calcula tudo
    suspend fun avaliarExpressao(expressao: String): Double // Devolve o resultado final da expressão
}