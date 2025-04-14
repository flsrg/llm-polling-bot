package dev.flsrg.bot.uitls

import dev.flsrg.bot.LlmPollingBot
import dev.flsrg.bot.roleplay.LanguageDetector
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class CallbackHelper(private val llmPollingBot: LlmPollingBot) {
    companion object {
        const val CALLBACK_DATA_FORCE_STOP = "FORCESTOP"
        const val CALLBACK_DATA_CLEAR_HISTORY = "CLEARHISTORY"
    }
    
    fun handleCallbackQuery(update: Update, language: LanguageDetector.Language) {
        val callback = update.callbackQuery
        val chatId = callback.message.chatId.toString()
        val callbackId = callback.id
        val userId = callback.from.id

        when (callback.data) {
            CALLBACK_DATA_FORCE_STOP -> {
                forceStop(chatId, callbackId, language)
            }
            CALLBACK_DATA_CLEAR_HISTORY -> {
                forceStop(chatId, callbackId, language)
                clearHistory(userId, chatId, callbackId, language)
            }
        }
    }

    private fun forceStop(chatId: String, callbackId: String, language: LanguageDetector.Language) = llmPollingBot.apply {
        val job = chatJobs[chatId]

        try {
            if (job != null) {
                job.cancel(BotUtils.UserStoppedException())

                onExecute(
                    AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackId)
                        .text(Strings.CallbackStopSuccessAnswer.get(language))
                        .build()
                )
            } else {
                onExecute(
                    AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackId)
                        .text(Strings.CallbackStopNothingRunningAnswer.get(language))
                        .build()
                )
            }
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun clearHistory(userId: Long, chatId: String, callbackId: String, language: LanguageDetector.Language) = llmPollingBot.apply {
        // Clear the chat history
        historyManager.clearHistory(userId)

        // Send confirmation to the user
        onExecute(
            AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(Strings.CallbackClearHistorySuccessAnswer.get(language))
                .build()
        )

        // Optionally, send a message to the chat confirming the history is cleared
        try {
            onExecute(
                SendMessage.builder()
                    .chatId(chatId)
                    .text(Strings.CallbackClearHistorySuccessMessage.get(language))
                    .build()
            )
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}