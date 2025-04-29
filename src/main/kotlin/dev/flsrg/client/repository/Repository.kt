package dev.flsrg.client.repository

import dev.flsrg.client.client.ClientConfig
import dev.flsrg.client.client.Model
import kotlinx.coroutines.flow.Flow

interface Repository {
    fun <T> getCompletionsStream(
        config: ClientConfig,
        model: Model,
        chatMessages: List<String>,
        transform: (String) -> T,
    ): Flow<T>
}