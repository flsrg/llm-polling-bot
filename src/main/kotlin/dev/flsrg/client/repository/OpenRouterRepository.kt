package dev.flsrg.client.repository

import dev.flsrg.client.api.Api
import dev.flsrg.client.client.ClientConfig
import dev.flsrg.client.client.Model
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory

class OpenRouterRepository(private val api: Api): Repository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun <T> getCompletionsStream(
        config: ClientConfig,
        model: Model,
        chatMessages: List<String>,
        transform: (String) -> T,
    ): Flow<T> {
        return flow {
            api.getCompletionsStream(model, chatMessages).collect { response ->
                log.info("Received API response (code={})", response.status.value)

                val channel: ByteReadChannel = response.bodyAsChannel()
                val isScopeActive = currentCoroutineContext().isActive

                while (!channel.isClosedForRead && isScopeActive) {
                    val line = channel.readUTF8Line() ?: break

                    if (line.startsWith("data: ")) {

                        val json = line.removePrefix("data: ").trim()
                        if (json == "[DONE]") break
                        validate(json)

                        emit(transform(json))
                    }
                }
            }
        }
    }

    private fun validate(json: String) {
        if (json.contains("error")) {
            val error = Json.parseToJsonElement(json).jsonObject["error"]
            if (error != null) throw Exception("API error: $error")
        }
    }
}