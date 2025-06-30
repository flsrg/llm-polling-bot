package dev.flsrg.bot.uitls

import dev.flsrg.bot.LlmPollingBot
import dev.flsrg.bot.uitls.BotUtils.botMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import java.util.concurrent.ConcurrentHashMap

class ProcessingMessageHelper(private val bot: LlmPollingBot) {
    private val processingMessages = ConcurrentHashMap<Long, Int>()

    fun sendProcessingMessage(message: String, chatId: Long, buttons: List<BotUtils.KeyboardButton>? = null) {
        val messageId = bot.execute(
            botMessage(
                chatId = chatId.toString(),
                message = message,
                buttons = buttons,
            )
        ).messageId

        processingMessages[chatId] = messageId
    }

    fun removeLastProcessingMessage(chatId: Long) {
        bot.execute(
            DeleteMessages.builder()
                .chatId(chatId)
                .messageId(processingMessages[chatId]!!)
                .build()
        )

        processingMessages.remove(chatId)
    }

    fun removeButtonsInLastProcessingMessage(chatId: Long) {
        bot.execute(
            EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(processingMessages[chatId]!!)
                .replyMarkup(null)
                .build()
        )
    }
}