package dev.flsrg.client.api

import dev.flsrg.client.ApiConfig
import dev.flsrg.client.client.ClientConfig
import dev.flsrg.client.client.Model
import dev.flsrg.client.model.ChatMessage
import dev.flsrg.client.model.ChatRequest
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

class OpenRouterApi(clientConfig: ClientConfig, apiConfig: ApiConfig): Api(clientConfig, apiConfig) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getCompletionsStream(model: Model, messagesJson: List<String>) = flow {
        val requestPayload = ChatRequest(
            model = model.id,
            chainOfThought = model.reasoning,
            stream = true,
            messages = getMessages(messagesJson)
        )

        val messagesCount = getMessagesCount(requestPayload.messages)
        log.info("Requesting completions from OpenRouter " +
                "(${messagesCount.first} userMessages, ${messagesCount.second} assistantMessages)")

        apiConfig.streamingClient.preparePost(clientConfig.baseUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${clientConfig.apiKey}")
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Accept, "text/event-stream")
            }
            setBody(requestPayload)
        }.execute { response ->
            emit(response)
        }
    }

    private fun getMessages(messagesJson: List<String>) = messagesJson.map {
        apiConfig.format.decodeFromString<ChatMessage>(it)
    }

    private fun getMessagesCount(messages: List<ChatMessage>): Pair<Int, Int> {
        val userMessages = messages.count { it.role == "user" }
        val assistantMessages = messages.count { it.role == "assistant" }
        return Pair(userMessages, assistantMessages)
    }
}