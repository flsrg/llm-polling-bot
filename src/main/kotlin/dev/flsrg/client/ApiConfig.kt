package dev.flsrg.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

interface ApiConfig {
    val format: Json
    val streamingClient: HttpClient
}

object DefaultApiConfig : ApiConfig {
    private const val API_CONNECTION_TIMEOUT_MS = 5 * 60 * 1000L

    override val format: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val streamingClient = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(format)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = API_CONNECTION_TIMEOUT_MS
        }
        install(SSE)
        engine {
            requestTimeout = 0
            endpoint {
                connectTimeout = API_CONNECTION_TIMEOUT_MS
                socketTimeout = 0
            }
        }
    }
}