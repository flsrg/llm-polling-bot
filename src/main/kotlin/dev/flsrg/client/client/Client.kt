package dev.flsrg.client.client

import dev.flsrg.client.model.ChatMessage
import dev.flsrg.client.model.ChatResponse
import kotlinx.coroutines.flow.Flow

abstract class Client(internal val config: ClientConfig) {
    abstract fun askChat(
        model: Model,
        messages: List<ChatMessage>,
        systemMessage: ChatMessage? = null,
    ): Flow<ChatResponse>
}