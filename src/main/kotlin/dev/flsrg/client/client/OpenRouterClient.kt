package dev.flsrg.client.client

import dev.flsrg.client.ApiConfig
import dev.flsrg.client.DefaultApiConfig
import dev.flsrg.client.api.OpenRouterApi
import dev.flsrg.client.model.ChatMessage
import dev.flsrg.client.model.ChatResponse
import dev.flsrg.client.repository.OpenRouterRepository
import kotlinx.coroutines.flow.Flow

class OpenRouterClient(config: ClientConfig, apiConfig: ApiConfig = DefaultApiConfig): Client(config, apiConfig) {
    private val api = OpenRouterApi(config, apiConfig)
    private val repository = OpenRouterRepository(api)

    override fun askChat(model: Model, messages: List<ChatMessage>, systemMessage: ChatMessage?): Flow<ChatResponse> {
        val payload: List<String> = if (systemMessage != null) {
            listOf(systemMessage) + messages
        } else {
            messages
        }.map {
            api.apiConfig.format.encodeToString(it)
        }

        return repository.getCompletionsStream(clientConfig, model, payload) {
            api.apiConfig.format.decodeFromString<ChatResponse>(it)
        }
    }
}