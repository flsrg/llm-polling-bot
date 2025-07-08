package dev.flsrg.bot.uitls

import dev.flsrg.bot.BotConfig
import dev.flsrg.bot.BotHandler
import dev.flsrg.bot.roleplay.LanguageDetector
import dev.flsrg.bot.uitls.BotUtils.withRetry
import dev.flsrg.client.model.ChatResponse
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException

class MessageProcessor(
    private val botConfig: BotConfig,
    private val botHandler: BotHandler,
    private val chatId: String,
) {
    companion object {
        private const val MARKDOWN_ERROR_MESSAGE = "can't parse entities"
        private const val MAX_MESSAGE_SKIPPED_TIMES = 2
    }

    private val contentBuffer = StringBuilder()
    private val reasoningBuffer = StringBuilder()
    private var contentMessageId: Int? = null
    private val reasoningMessageIds = linkedSetOf<Int?>()
    private var finalAssistantMessage = StringBuilder()

    private var messageSkippedTimes = 0

    suspend fun processMessage(message: ChatResponse) {
        message.choices.firstOrNull()?.delta?.let { delta ->
            delta.reasoning?.let { handleReasoning(it) }
            delta.content?.let { handleContent(it) }
        }
    }

    private suspend fun handleReasoning(reasoning: String) {
        reasoningBuffer.append(reasoning)

        if (reasoningBuffer.length > botConfig.messageMaxLength) {
            sendReasoning(isLastMessage = true)
        }
    }

    private suspend fun handleContent(content: String) {
        finalAssistantMessage.append(content)
        contentBuffer.append(content)

        if (contentBuffer.length > botConfig.messageMaxLength) {
            sendContent(isLastMessage = true, skipIfSendFailure = true)
        }
    }

    suspend fun updateOrSend(buttons: List<BotUtils.KeyboardButton>, language: LanguageDetector.Language, isCompletion: Boolean) {
        when {
            contentBuffer.isNotEmpty() -> {
                clearReasoning(language)
                sendContent(buttons.toList(), isCompletion = isCompletion)
            }
            reasoningBuffer.isNotEmpty() -> sendReasoning(buttons.toList())
        }
    }

    private fun clearReasoning(language: LanguageDetector.Language) {
        reasoningBuffer.clear()
        if (reasoningMessageIds.isNotEmpty()) {
            deleteAllReasoningMessages()
            reasoningMessageIds.clear()
            botHandler.sendMessage(
                chatId = chatId,
                message = Strings.ThinkingCompletedMessage.get(language),
            )
        }
    }

    private suspend fun sendReasoning(
        buttons: List<BotUtils.KeyboardButton> = emptyList(),
        isLastMessage: Boolean = false,
    ) {
        val reasoningMessageId = updateOrSendMessage(
            message = reasoningBuffer.toString(),
            existingMessageId = reasoningMessageIds.lastOrNull(),
            parseMode = null,
            keyboardButtons = buttons,
        )
        reasoningMessageIds.add(reasoningMessageId)

        if (isLastMessage) {
            reasoningBuffer.clear()
            reasoningMessageIds.remove(null)
            reasoningMessageIds.add(null)
        }
    }

    private suspend fun sendContent(
        buttons: List<BotUtils.KeyboardButton> = emptyList(),
        isNeedFormatting: Boolean = true,
        skipIfSendFailure: Boolean = false,
        isLastMessage: Boolean = false,
        isCompletion: Boolean = false,
    ) {
        val (safeContentMessage, remaining) = splitAtLastMarkdownSymbol(contentBuffer)

        try {
            contentMessageId = updateOrSendMessage(
                message = safeContentMessage,
                existingMessageId = contentMessageId,
                keyboardButtons = buttons,
                parseMode = if (isNeedFormatting) botConfig.botMessageParseMode else null
            )
        } catch (e: TelegramApiRequestException) {
            if (e.errorCode == BotConfig.BAD_REQUEST_ERROR_CODE && e.message?.contains(MARKDOWN_ERROR_MESSAGE) == true) {
                if (skipIfSendFailure && messageSkippedTimes < MAX_MESSAGE_SKIPPED_TIMES) {
                    messageSkippedTimes++
                    return
                } else {
                    sendContent(buttons, isNeedFormatting = false, isCompletion = isCompletion)
                }
            }
        }

        if (isLastMessage) {
            contentBuffer.clear()
            contentBuffer.append(remaining)
            contentMessageId = null
        }

        if (isCompletion && remaining.isNotEmpty()) {
            updateOrSendMessage(
                message = remaining,
                existingMessageId = null,
                keyboardButtons = buttons,
                parseMode = null
            )
        }
    }

    private var prevMessage: String? = null

    /**
     * @return existing active editable message id
     */
    private suspend fun updateOrSendMessage(
        message: String,
        existingMessageId: Int?,
        parseMode: String? = botConfig.botMessageParseMode,
        keyboardButtons: List<BotUtils.KeyboardButton> = emptyList(),
    ): Int? {
        if (message.isEmpty()) return existingMessageId

        val messageId = withRetry(maxRetries = 5, initialDelay = 5000, origin = "execute updateOrSendMessage") {
            if (existingMessageId == null) {
                return@withRetry botHandler.sendMessage(
                    chatId = chatId,
                    message = message,
                    buttons = keyboardButtons,
                    parseMode = parseMode,
                )

            } else {
                if (message == prevMessage) return@withRetry existingMessageId
                botHandler.editMessage(
                    chatId = chatId,
                    messageId = existingMessageId,
                    message = message,
                    buttons = keyboardButtons,
                    parseMode = parseMode
                )

                return@withRetry existingMessageId
            }
        }

        prevMessage = message
        return messageId
    }

    private fun deleteAllReasoningMessages() {
        reasoningMessageIds.mapNotNull { it }.takeIf { it.isNotEmpty() }?.let { ids ->
            botHandler.deleteMessages(
                chatId = chatId,
                messageIds = ids,
            )
        }
    }

    fun getFinalAssistantMessage(): String = finalAssistantMessage.toString()

    private fun isEscaped(text: String, index: Int): Boolean {
        if (index <= 0) return false
        var count = 0
        var i = index - 1
        while (i >= 0 && text[i] == '\\') {
            count++
            i--
        }
        return count % 2 == 1
    }

    private fun splitAtLastMarkdownSymbol(stringBuilder: StringBuilder): SafeText {
        val text = stringBuilder.toString()
        if (text.isEmpty()) {
            return SafeText("", "")
        }

        val markdownSymbols = listOf(
            "```", "~~~", "~~", "**", "__", "![", "---", "***", "===",
            "`", "*", "_"
        ).sortedByDescending { it.length }

        var i = text.length - 1
        while (i >= 0) {
            for (symbol in markdownSymbols) {
                if (i + symbol.length <= text.length) {
                    if (text.startsWith(symbol, i)) {
                        if (!isEscaped(text, i)) {
                            val safeEnd = i + symbol.length
                            return SafeText(text.substring(0, safeEnd), text.substring(safeEnd))
                        }
                    }
                }
            }
            i--
        }
        return SafeText(text, "")
    }

    internal data class SafeText(val safeText: String, val remaining: String)
}