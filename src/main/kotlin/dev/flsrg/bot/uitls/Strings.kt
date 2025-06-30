package dev.flsrg.bot.uitls

import dev.flsrg.bot.roleplay.LanguageDetector

sealed class Strings(
    private val ru: String,
    private val en: String,
) {
    fun get(lang: LanguageDetector.Language, vararg args: Any): String = when (lang) {
        LanguageDetector.Language.EN -> en.format(*args)
        LanguageDetector.Language.RU -> ru.format(*args)
    }

    data object StartMessage : Strings("Го", "Let's go")
    data object ThinkingMessage : Strings("Думаю...", "Thinking...")
    data object ThinkingCompletedMessage : Strings("Подумал, получается:", "Thought and it's:")
    data object ProcessingMessage : Strings("⏳Обработка", "⏳Processing")
    data object RateLimitMessage : Strings("Превышен лимит запросов. Подожди", "Rate limit exceeded. Wait")
    data object KeyboardStopText : Strings("🚫 Остановись", "🚫 Stop it")
    data object KeyboardClearHistoryText : Strings("🧹 Забудь все", "🧹 Forgot all")
    data object KeyboardRegenerate : Strings("↻", "↻")

    data object CallbackStopSuccessAnswer : Strings("Остановился!", "Stopped!")
    data object CallbackStopNothingRunningAnswer : Strings("Нечего останавливать", "Nothing to stop")
    data object CallbackClearHistorySuccessAnswer : Strings("Чисто!", "History cleared!")
    data object CallbackClearHistorySuccessMessage : Strings("Бот забыл историю. Давай по новой", "Bot forgot the history. Let's start over")

    data object StopErrorUser : Strings("Стою", "I'm stopped")
    data object StopErrorNewMessage : Strings("Новое сообщение в чате, так, ща...", "There is a new message in the chat, so...")
}