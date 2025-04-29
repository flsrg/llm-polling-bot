package dev.flsrg.client.api

import dev.flsrg.client.ApiConfig
import dev.flsrg.client.client.ClientConfig
import dev.flsrg.client.client.Model
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.flow.Flow

abstract class Api(
    protected val clientConfig: ClientConfig,
    val apiConfig: ApiConfig
) {
    abstract fun getCompletionsStream(model: Model, messagesJson: List<String>): Flow<HttpResponse>
}