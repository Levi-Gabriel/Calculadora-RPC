# Calculadora JSON-RPC (Kotlin + Ktor)

Calculadora cliente/servidor via HTTP/JSON (`http://localhost:8080/rpc`).

## Requisitos

- JDK 17 ou superior
- Gradle (ou o wrapper `./gradlew` do projeto)

## Estrutura de pastas

```
projeto/
├── settings.gradle.kts
├── build.gradle.kts
└── src/main/kotlin/com/rpc/
    ├── CalculatorService.kt
    ├── Evaluator.kt
    ├── Server.kt
    └── Client.kt
```

## `build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
}

repositories { mavenCentral() }

val ktor = "2.3.12"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-client-core:$ktor")
    implementation("io.ktor:ktor-client-java:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

kotlin { jvmToolchain(17) }

tasks.register<JavaExec>("runServer") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.rpc.ServerKt")
}

tasks.register<JavaExec>("runClient") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.rpc.ClientKt")
    standardInput = System.`in`
}
```

E no `settings.gradle.kts`:

```kotlin
rootProject.name = "rpc"
```

## Como rodar

**1. Inicie o servidor** (em um terminal):

```bash
gradle runServer --console=plain
```

**2. Inicie o cliente** (em outro terminal):

```bash
gradle runClient --console=plain
```

**3. Escolha uma opção no menu:**

| Opção | O que faz | Exemplo |
|-------|-----------|---------|
| 1 | Operação simples | `10 + 15` |
| 2 | Expressão calculada no cliente (uma chamada remota por operação) | `(10 + 15) * 4` |
| 3 | Expressão calculada no servidor (uma única chamada remota) | `(10 + 15) * 4` |
| 0 | Sair | |

> Dica: no IntelliJ, também dá para rodar direto pelo ▶ ao lado do `main` de `Server.kt` e `Client.kt`.

## Rodando em máquinas diferentes

No `Client.kt`, troque o endereço pelo IP da máquina do servidor:

```kotlin
CalculatorRpcClient(httpClient, "http://192.168.0.10:8080/rpc")
```

Libere a porta `8080` no firewall do servidor, se necessário.
