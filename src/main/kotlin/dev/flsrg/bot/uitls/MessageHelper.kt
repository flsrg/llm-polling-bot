package dev.flsrg.bot.uitls

import dev.flsrg.bot.LlmPollingBot
import dev.flsrg.bot.roleplay.LanguageDetector
import dev.flsrg.bot.uitls.BotUtils.botMessage
import dev.flsrg.bot.uitls.BotUtils.decapitalizeFirstChar
import dev.flsrg.bot.uitls.BotUtils.editMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.api.objects.Message

class MessageHelper(private val llmPollingBot: LlmPollingBot, private val chatId: String) {
    companion object {
        private const val START_DEFAULT_COMMAND = "/start"

        val RU_THINKING_PREFIX = listOf("Подумай", "Думай")
        val EN_THINKING_PREFIX = listOf("Think")
        private val THINKING_PREFIX = RU_THINKING_PREFIX + EN_THINKING_PREFIX
        private const val THINKING_PREFIX_RANGE = 20

        fun isStartMessage(message: Message): Boolean {
            return message.text == START_DEFAULT_COMMAND
        }

        fun isThinkingMessage(message: Message): Boolean {
            val messageRange = message.text
                .take(THINKING_PREFIX_RANGE)
                .decapitalizeFirstChar()

            return THINKING_PREFIX.any { prefix ->
                messageRange.contains(
                    prefix.decapitalizeFirstChar()
                )
            }
        }

        fun sendStartMessage(bot: LlmPollingBot, chatId: String, language: LanguageDetector.Language) = apply {
            bot.onExecute(botMessage(chatId, Strings.StartMessage.get(language)))
        }

        fun sendRateLimitMessage(bot: LlmPollingBot, chatId: String, language: LanguageDetector.Language) = apply {
            bot.onExecute(
                botMessage(
                    chatId = chatId,
                    message = Strings.RateLimitMessage.get(language)
                )
            )
        }
    }

    private lateinit var thinkingMessage: Pair<Int, String>
    private var processingMessageId: Int? = null

    fun sendProcessingMessage(language: LanguageDetector.Language) = llmPollingBot.apply {
        val message = Strings.ProcessingMessage.get(language)
        val messageId = onExecute(
            botMessage(
                chatId = chatId,
                message = message,
            )
        ).messageId

        processingMessageId = messageId
    }

    fun deleteProcessingMessage() = llmPollingBot.apply {
        onExecute(
            DeleteMessages.builder()
                .chatId(chatId)
                .messageId(processingMessageId!!)
                .build()
        )
    }

    fun sendThinkingMessage(language: LanguageDetector.Language) = llmPollingBot.apply {
        val message = Strings.ThinkingMessage.get(language)

        val messageId = onExecute(
            botMessage(
                chatId = chatId,
                message = message,
                buttons = listOf(BotUtils.KeyboardButtonStop(language))
            )
        ).messageId

        thinkingMessage = messageId to message
    }

    fun cleanupThinkingMessage() = llmPollingBot.apply {
        onExecute(
            editMessage(
                chatId = chatId,
                messageId = thinkingMessage.first,
                message = thinkingMessage.second,
                buttons = null
            )
        )
    }
}