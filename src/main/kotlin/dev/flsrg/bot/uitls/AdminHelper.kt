package dev.flsrg.bot.uitls

import dev.flsrg.bot.BotHandler
import dev.flsrg.bot.repo.UserRepository
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.*

class AdminHelper(
    private val botHandler: BotHandler,
    private val adminUserId: Long,
    private val userRepository: UserRepository,
) {
    companion object {
        private const val ADMIN_STATS_COMMAND = "/stats"
        private const val CALLBACK_DATA_USERS_LIST = "USERSLIST"
    }

    fun isAdminCommand(update: Update) = update.message.text == ADMIN_STATS_COMMAND
    fun isAdminCallback(update: Update) = update.callbackQuery.data == CALLBACK_DATA_USERS_LIST

    fun handleAdminCommand(update: Update) {
        if (update.message.from.id != adminUserId) return

        if (update.message.text == ADMIN_STATS_COMMAND) {
            sendStatistics(update.message.chatId.toString())
        }
    }

    fun updateUserMessage(userId: Long, userName: String) {
        userRepository.recordMessage(userId, userName)
    }

    private fun sendStatistics(adminChatId: String) {
        try {
            val stats = """
                📊 *Bot Statistics Report*
                
                👥 *Total Users:* ${userRepository.getTotalUserCount()}
                🟢 *Active Users (30 days):* ${userRepository.getActiveUsers(30).count()}
                💬 *Total Messages Processed:* ${userRepository.getTotalMessageCount()}
                📈 *Daily Messages (Avg):* ${getDailyMessageAverage()}
                👤 *Most Active User:* ${getMostActiveUser()}
            """.trimIndent()

            botHandler.sendMessage(
                chatId = adminChatId,
                message = stats,
                parseMode = ParseMode.MARKDOWN,
                buttons = listOf(UsersListKeyboardButton())
            )
        } catch (e: Exception) {
            botHandler.sendMessage(
                chatId = adminChatId,
                message = "❌ Error generating statistics: ${e.message}"
            )
            throw e
        }
    }

    private fun getDailyMessageAverage(): String {
        val totalMessages = userRepository.getTotalMessageCount()
        val daysActive = (System.currentTimeMillis() - userRepository.getOldestUserDate()) / (24 * 60 * 60 * 1000)

        return "%.1f".format(totalMessages.toDouble() / daysActive)
    }

    private fun getMostActiveUser(): String {
        val mostActive = userRepository.getUsers().maxBy {
            it.messageCount
        }
        return "${mostActive.username ?: "Anonymous"} (${mostActive.messageCount} messages)"
    }

    private class UsersListKeyboardButton : BotUtils.KeyboardButton(
        "Show users list",
        CALLBACK_DATA_USERS_LIST
    )

    fun handleCallbackQuery(update: Update) {
        val callback = update.callbackQuery
        val chatId = callback.message.chatId.toString()

        when (callback.data) {
            CALLBACK_DATA_USERS_LIST -> {
                sendUsersList(chatId, callback.id)
            }
        }
    }

    private fun sendUsersList(chatId: String, callbackId: String) {
        val users = userRepository.getUsers().joinToString("\n") {
            """
                👤 *User ${it.username}*
                
                - *id:* ${it.id}
                - *message count:* ${it.messageCount}
                - *last seen:* ${Date(it.lastActive)}
                
                
            """.trimIndent()
        }

        botHandler.execute(
            AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text("Ok")
                .build()
        )

        botHandler.sendMessage(
            chatId = chatId,
            message = users,
            parseMode = ParseMode.MARKDOWN,
        )
    }
}