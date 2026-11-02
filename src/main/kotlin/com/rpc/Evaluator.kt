package com.rpc // Pacote do projeto

// Avaliador de expressões aritméticas (descida recursiva).
//  Cliente: recebe o stub RPC -> cada operação vira uma requisição HTTP/JSON ao servidor (abordagem 1)
//  Servidor: recebe a implementação local -> tudo é calculado sem rede (abordagem 2)
class Evaluator(expressao: String, private val calc: CalculatorService) {

    private val txt = expressao.replace(Regex("\\s+"), "") // Expressão sem espaços em branco
    private var pos = 0 // Posição atual de leitura no texto

    suspend fun avalia(): Double { // Ponto de entrada: avalia a expressão completa
        val v = expr() // Processa a regra de mais alto nível
        if (pos != txt.length) { // Se sobrou texto não consumido, a expressão é inválida
            throw IllegalArgumentException("Expressão inválida na posição $pos") // Informa o erro
        }
        return v // Retorna o resultado final
    }

    // expr = termo { (+|-) termo }
    private suspend fun expr(): Double {
        var v = termo() // Lê o primeiro termo
        while (pos < txt.length) { // Enquanto houver texto
            val c = txt[pos] // Caractere atual
            if (c != '+' && c != '-') break // Sai se não for + nem -
            pos++ // Consome o operador
            val d = termo() // Lê o próximo termo
            v = if (c == '+') calc.somar(v, d) else calc.subtrair(v, d) // Soma ou subtração (remota ou local)
        }
        return v // Retorna o valor acumulado
    }

    // termo = fator { (*|/) fator }
    private suspend fun termo(): Double {
        var v = fator() // Lê o primeiro fator
        while (pos < txt.length) { // Enquanto houver texto
            val c = txt[pos] // Caractere atual
            if (c != '*' && c != '/') break // Sai se não for * nem /
            pos++ // Consome o operador
            val d = fator() // Lê o próximo fator
            v = if (c == '*') calc.multiplicar(v, d) else calc.dividir(v, d) // Multiplicação ou divisão (remota ou local)
        }
        return v // Retorna o valor acumulado
    }

    // fator = número | '(' expr ')' | '-' fator
    private suspend fun fator(): Double {
        if (pos >= txt.length) throw IllegalArgumentException("Expressão incompleta") // Fim inesperado
        val c = txt[pos] // Caractere atual
        if (c == '-') { // Menos unário (ex.: -5)
            pos++ // Consome o '-'
            return calc.subtrair(0.0, fator()) // 0 - fator (inverte o sinal)
        }
        if (c == '(') { // Início de subexpressão entre parênteses
            pos++ // Consome o '('
            val v = expr() // Avalia a subexpressão
            if (pos >= txt.length || txt[pos] != ')') { // Exige o ')' correspondente
                throw IllegalArgumentException("Parêntese não fechado") // Erro de sintaxe
            }
            pos++ // Consome o ')'
            return v // Retorna o valor da subexpressão
        }
        val ini = pos // Marca o início do número
        while (pos < txt.length) { // Lê dígitos e ponto
            val d = txt[pos] // Caractere atual
            if (!d.isDigit() && d != '.') break // Sai se não for dígito nem ponto
            pos++ // Avança enquanto for parte do número
        }
        if (ini == pos) throw IllegalArgumentException("Número esperado na posição $pos") // Nenhum dígito lido
        return txt.substring(ini, pos).toDouble() // Converte o texto lido para Double
    }
}