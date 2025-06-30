package dev.flsrg.bot

import dev.flsrg.bot.db.Database
import dev.flsrg.bot.hist.HistoryManager
import dev.flsrg.bot.repo.SQLChatHistRepository
import dev.flsrg.bot.repo.SQLUsersRepository
import dev.flsrg.bot.roleplay.LanguageDetector
import dev.flsrg.bot.roleplay.LanguageDetector.Language.RU
import dev.flsrg.bot.uitls.*
import dev.flsrg.bot.uitls.BotUtils.botMessage
import dev.flsrg.bot.uitls.BotUtils.sendTypingAction
import dev.flsrg.bot.uitls.BotUtils.withRetry
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

open class LlmPollingBot(
    botToken: String?,
    adminUserId: Long,
    private val botUsername: String,
    private val botConfig: BotConfig,
) : TelegramLongPollingBot(botToken), BotHandler {
    companion object {
        private const val START_DEFAULT_COMMAND = "/start"
    }

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val usersRepository = SQLUsersRepository()
    private val adminHelper = AdminHelper(this, adminUserId, usersRepository)
    private val callbackHelper = CallbackHelper(this)
    val historyManager by lazy {
        HistoryManager(
            botConfig = botConfig,
            histRepository = SQLChatHistRepository(Database.database),
            usersRepository = usersRepository,
        )
    }
    protected val processingMessageHelper = ProcessingMessageHelper(this)

    private val rootScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val rateLimits = ConcurrentHashMap<String, Long>()
    private val lastUsedLanguage = ConcurrentHashMap<String, LanguageDetector.Language>()
    val chatJobs = ConcurrentHashMap<String, Job>()

    // Cleanup mechanism to remove completed jobs
    init {
        rootScope.launch {
            while (isActive) {
                delay(botConfig.jobCleanupInterval)
                chatJobs.entries.removeAll { (_, job) -> job.isCompleted }
            }
        }
        Database.init(botUsername)
    }

    override fun getBotUsername() = botUsername

    override fun <T : Serializable?, Method : BotApiMethod<T>?> onExecute(method: Method): T = execute(method)

    override fun onUpdateReceived(update: Update) {
        rootScope.launch(Dispatchers.IO) {
            if (update.hasMessage() && update.message.hasText()) {
                val chatId = update.message.chat.id.toString()
                when {
                    adminHelper.isAdminCommand(update) -> adminHelper.handleAdminCommand(update)
                    isStartMessage(update) -> execute(botMessage(chatId, Strings.StartMessage.get(lastUsedLanguage[chatId] ?: RU)))
                    else -> handleMessage(update)
                }

            } else if (update.hasCallbackQuery()) {
                val chatId = update.callbackQuery.message.chatId.toString()
                when {
                    adminHelper.isAdminCallback(update) -> adminHelper.handleCallbackQuery(update)
                    else -> callbackHelper.handleCallbackQuery(update, lastUsedLanguage[chatId] ?: RU)
                }
            }
        }
    }

    open fun handleMessage(update: Update) {
        val startMillis = System.currentTimeMillis()

        val userId = update.message.from.id
        val chatId = update.message.chat.id.toString()
        val userName = update.message.from.userName ?: "id: $userId"
        val userMessage = update.message.text
        val lang = LanguageDetector.detectLanguage(userMessage)
        lastUsedLanguage[chatId] = lang

        if (startMillis - rateLimits.getOrDefault(chatId, 0) < botConfig.messageRateLimit) {
            execute(botMessage(chatId, Strings.RateLimitMessage.get(lang)))
            return
        }
        rateLimits[chatId] = startMillis

        chatJobs[chatId]?.cancel(BotUtils.NewMessageStopException())

        val newJob = rootScope.launch {
            val messageProcessor = MessageProcessor(
                botConfig = botConfig,
                botHandler = this@LlmPollingBot,
                chatId = chatId,
            )

            try {
                withRetry(origin = "askLlm") {
                    sendTypingAction(chatId)
                    sendProcessingMessage(userMessage, chatId.toLong(), lang)
                    askLlm(messageProcessor, update.message, lang)
                }
            } catch (e: Exception) {
                val errorMessage = BotUtils.errorToMessage(e, lang)
                onExecute(botMessage(chatId, errorMessage))

                log.error("Error processing message", e)

            } finally {
                chatJobs.remove(chatId)
                log.info("Responding to $userName completed " +
                        "(${System.currentTimeMillis() - startMillis}ms)")
            }
        }

        chatJobs[chatId] = newJob
    }

    open suspend fun askLlm(messageProcessor: MessageProcessor, message: Message, language: LanguageDetector.Language) {
        TODO("Not yet implemented")
    }

    open fun sendProcessingMessage(userPrompt: String, chatId: Long, language: LanguageDetector.Language) {
        processingMessageHelper.sendProcessingMessage(Strings.ProcessingMessage.get(language), chatId)
    }

    private fun isStartMessage(update: Update): Boolean = update.message.text == START_DEFAULT_COMMAND
}