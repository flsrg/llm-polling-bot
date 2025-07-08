package dev.flsrg.bot

import dev.flsrg.bot.uitls.BotUtils.KeyboardButton
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.Serializable

interface BotHandler {
    /**
     * @return message Id
     */
    fun sendMessage(
        chatId: String,
        message: String,
        buttons: List<KeyboardButton>? = null,
        parseMode: String? = null,
    ): Int

    fun editMessage(
        chatId: String,
        messageId: Int,
        message: String,
        buttons: List<KeyboardButton>? = null,
        parseMode: String? = null
    )

    fun deleteMessages(
        chatId: String,
        messageIds: List<Int>,
    )

    @Throws(TelegramApiException::class)
    fun <T : Serializable?, Method : BotApiMethod<T>?> execute(method: Method): T
}