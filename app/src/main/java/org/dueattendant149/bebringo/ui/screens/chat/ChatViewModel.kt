package org.dueattendant149.bebringo.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.data.llm.LlmClient
import org.dueattendant149.bebringo.data.llm.LlmConfig
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.domain.model.ChatMessage
import org.dueattendant149.bebringo.domain.model.ChatScenario
import java.util.UUID

data class ChatUiState(
    val configured: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val review: String? = null,
    val error: String? = null,
) {
    val canFinish: Boolean get() = messages.count { it.fromUser } >= 3
}

/**
 * Чат по сценарию. Ошибки не исправляются в моменте: носитель переформулирует.
 * Разбор — по кнопке, после нескольких реплик.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val llm = LlmClient(ServiceLocator.settings)
    private var config: LlmConfig = LlmConfig("", "", "")

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun start(scenario: ChatScenario) {
        viewModelScope.launch {
            config = llm.config()
            _state.update { current ->
                current.copy(
                    configured = config.isConfigured,
                    messages = if (current.messages.isEmpty()) {
                        listOf(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = openingFor(scenario),
                                fromUser = false,
                                timeLabel = nowLabel(),
                            ),
                        )
                    } else {
                        current.messages
                    },
                )
            }
        }
    }

    fun send(scenario: ChatScenario, text: String) {
        if (text.isBlank() || !config.isConfigured) return
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            fromUser = true,
            timeLabel = nowLabel(),
        )
        val history = _state.value.messages + userMessage
        _state.update { it.copy(messages = history, sending = true, error = null) }

        viewModelScope.launch {
            runCatching { llm.reply(config, scenario, history) }
                .onSuccess { answer ->
                    _state.update {
                        it.copy(
                            messages = it.messages + ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = answer.trim(),
                                fromUser = false,
                                timeLabel = nowLabel(),
                            ),
                            sending = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(sending = false, error = error.message) }
                }
        }
    }

    fun finish() {
        if (!config.isConfigured) return
        val history = _state.value.messages
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            runCatching { llm.review(config, history) }
                .onSuccess { review -> _state.update { it.copy(sending = false, review = review) } }
                .onFailure { error -> _state.update { it.copy(sending = false, error = error.message) } }
        }
    }

    fun dismissReview() = _state.update { it.copy(review = null) }

    private fun openingFor(scenario: ChatScenario): String = when (scenario.titleRo) {
        "La gară" -> "Bună ziua! Cu ce vă pot ajuta?"
        "La piață" -> "Bună ziua! Ce doriți astăzi?"
        "La cafenea" -> "Bună! Ce vă aduc?"
        else -> "Bună ziua!"
    }

    private fun nowLabel(): String {
        val calendar = java.util.Calendar.getInstance()
        return "%02d:%02d".format(calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
    }
}
