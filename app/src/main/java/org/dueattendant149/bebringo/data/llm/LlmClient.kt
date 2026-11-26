package org.dueattendant149.bebringo.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.dueattendant149.bebringo.data.prefs.SettingsStore
import org.dueattendant149.bebringo.domain.model.ChatMessage
import org.dueattendant149.bebringo.domain.model.ChatScenario
import java.util.concurrent.TimeUnit

/** Настройки подключения к модели. */
data class LlmConfig(
    val endpoint: String,
    val model: String,
    val apiKey: String,
) {
    val isConfigured: Boolean get() = endpoint.isNotBlank() && apiKey.isNotBlank()
}

class LlmException(message: String) : Exception(message)

/**
 * Клиент чата с носителем.
 *
 * Работает с любым OpenAI-совместимым эндпоинтом: пользователь сам указывает адрес,
 * модель и ключ. Без ключа чат честно недоступен, остальное приложение работает.
 */
class LlmClient(private val settings: SettingsStore) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun config(): LlmConfig = LlmConfig(
        endpoint = settings.llmEndpoint.first(),
        model = settings.llmModel.first(),
        apiKey = settings.llmKey.first(),
    )

    /** Ответ собеседника в рамках сценария. */
    suspend fun reply(
        config: LlmConfig,
        scenario: ChatScenario,
        history: List<ChatMessage>,
    ): String {
        val messages = buildList {
            add(message("system", systemPrompt(scenario)))
            history.forEach { add(message(if (it.fromUser) "user" else "assistant", it.text)) }
        }
        return complete(config, messages)
    }

    /** Разбор после сценария: до трёх ошибок. */
    suspend fun review(
        config: LlmConfig,
        history: List<ChatMessage>,
    ): String {
        val transcript = history.joinToString("\n") { "${if (it.fromUser) "Ученик" else "Носитель"}: ${it.text}" }
        val messages = listOf(
            message(
                "system",
                "Ты преподаватель румынского. Дан диалог ученика с носителем. " +
                    "Найди не больше трёх ошибок ученика. Ответь по-русски, коротко, " +
                    "в формате: «было → правильно — объяснение». Если ошибок нет, напиши «Ошибок нет».",
            ),
            message("user", transcript),
        )
        return complete(config, messages)
    }

    private suspend fun complete(config: LlmConfig, messages: List<JsonObject>): String =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject {
                put("model", config.model)
                put("temperature", 0.7)
                put("messages", JsonArray(messages))
            }

            val request = Request.Builder()
                .url(chatUrl(config.endpoint))
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer ${config.apiKey}")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw LlmException("Модель ответила ${response.code}: ${body.take(200)}")
                }
                val root = json.parseToJsonElement(body).jsonObject
                val content = root["choices"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                content ?: throw LlmException("Пустой ответ модели")
            }
        }

    /** Дополняет адрес до полного пути, если пользователь вставил только базу. */
    private fun chatUrl(endpoint: String): String {
        val trimmed = endpoint.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
            else -> "$trimmed/v1/chat/completions"
        }
    }

    private fun message(role: String, content: String): JsonObject = buildJsonObject {
        put("role", role)
        put("content", content)
    }

    private fun systemPrompt(scenario: ChatScenario): String = """
        Ты играешь роль носителя румынского в сценарии «${scenario.titleRo}».
        Правила:
        - отвечай только по-румынски, короткими фразами уровня A1–A2;
        - не исправляй ошибки напрямую и не читай нотаций;
        - если ученик ошибся, естественно переформулируй его мысль правильно и продолжи разговор;
        - держись сценария, но реагируй живо;
        - не больше двух предложений за ход;
        - никогда не используй английский.
    """.trimIndent()

}
