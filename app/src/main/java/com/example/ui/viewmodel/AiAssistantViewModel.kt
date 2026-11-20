package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.GeminiModelOption
import com.example.data.repository.AiStructuredResponse
import com.example.data.repository.CreovaAiRepository
import com.example.domain.model.CreativeResource
import com.example.domain.model.CreativeTool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AiChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val structuredResponse: AiStructuredResponse? = null
)

sealed interface AiAssistantUiState {
    data object Idle : AiAssistantUiState
    data object Loading : AiAssistantUiState
    data class Streaming(val partialText: String) : AiAssistantUiState
    data object Ready : AiAssistantUiState
    data class Error(val message: String, val lastPrompt: String) : AiAssistantUiState
}

class AiAssistantViewModel(
    private val aiRepository: CreovaAiRepository = CreovaAiRepository()
) : ViewModel() {

    private companion object {
        const val TAG = "CreovaAiVM"
    }

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<AiAssistantUiState>(AiAssistantUiState.Idle)
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private val historyPairs = mutableListOf<Pair<String, String>>()

    fun sendChatPrompt(
        prompt: String,
        modelOption: GeminiModelOption,
        tools: List<CreativeTool>,
        resources: List<CreativeResource>,
        requestStructured: Boolean = false
    ) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        val userMessage = AiChatMessage(
            id = "u_${System.currentTimeMillis()}",
            isFromUser = true,
            text = clean
        )
        _messages.value = _messages.value + userMessage
        _uiState.value = AiAssistantUiState.Loading

        viewModelScope.launch {
            if (requestStructured) {
                aiRepository.generateStructuredPicks(modelOption, clean, tools, resources)
                    .onSuccess { structured ->
                        val replyText = structured.summary
                        historyPairs.add(clean to replyText)
                        _messages.value = _messages.value + AiChatMessage(
                            id = "a_${System.currentTimeMillis()}",
                            isFromUser = false,
                            text = replyText,
                            structuredResponse = structured
                        )
                        _uiState.value = AiAssistantUiState.Ready
                    }
                    .onFailure { error ->
                        val msg = if (error is TimeoutCancellationException) {
                            "الطلب استغرق وقتاً أطول من اللازم (60 ثانية). حاول مرة أخرى."
                        } else {
                            error.localizedMessage ?: "تعذر الاتصال بمساعد CREOVA AI."
                        }
                        _uiState.value = AiAssistantUiState.Error(msg, clean)
                    }
                return@launch
            }

            var latest = ""
            aiRepository.streamChat(
                modelOption = modelOption,
                historyPairs = historyPairs.toList(),
                userMessage = clean,
                tools = tools,
                resources = resources
            ).catch { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                Log.e(TAG, "AI streaming error", error)
                val msg = if (error is TimeoutCancellationException) {
                    "انتهت مهلة الاستجابة (60 ثانية). حاول مرة أخرى."
                } else {
                    error.localizedMessage ?: "حدث خطأ أثناء الاتصال بـ Gemini."
                }
                _uiState.value = AiAssistantUiState.Error(msg, clean)
            }.collect { chunk ->
                latest = chunk
                _uiState.value = AiAssistantUiState.Streaming(chunk)
            }

            if (latest.isNotBlank() && _uiState.value is AiAssistantUiState.Streaming) {
                historyPairs.add(clean to latest)
                _messages.value = _messages.value + AiChatMessage(
                    id = "a_${System.currentTimeMillis()}",
                    isFromUser = false,
                    text = latest
                )
                _uiState.value = AiAssistantUiState.Ready
            }
        }
    }

    fun clearConversation() {
        historyPairs.clear()
        _messages.value = emptyList()
        _uiState.value = AiAssistantUiState.Idle
    }
}
