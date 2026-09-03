package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.PhoneAction
import com.example.actions.PhoneActionExecutor
import com.example.ai.HinglishCommandParser
import com.example.data.AppDatabase
import com.example.data.AssistantRepository
import com.example.data.ChatMessageEntity
import com.example.data.NotificationItemEntity
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ExecutionMode {
    SAFE_CONFIRMATION, // Sensitive actions require confirmation
    INSTANT_ACTION     // Actions executed immediately
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AssistantRepository
    private val actionExecutor: PhoneActionExecutor
    var voiceManager: VoiceAssistantManager? = null

    val messages: StateFlow<List<ChatMessageEntity>>
    val notifications: StateFlow<List<NotificationItemEntity>>

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _executionMode = MutableStateFlow(ExecutionMode.SAFE_CONFIRMATION)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _isHandsFreeMode = MutableStateFlow(true)
    val isHandsFreeMode: StateFlow<Boolean> = _isHandsFreeMode.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _showNotificationSheet = MutableStateFlow(false)
    val showNotificationSheet: StateFlow<Boolean> = _showNotificationSheet.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AssistantRepository(db.assistantDao())
        actionExecutor = PhoneActionExecutor(application)

        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        notifications = repository.allNotifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            checkAndSeedInitialData()
        }
    }

    private suspend fun checkAndSeedInitialData() {
        val existing = repository.allMessages.first()
        if (existing.isEmpty()) {
            repository.insertMessage(
                ChatMessageEntity(
                    sender = "Ai ✨ Assistant",
                    text = "Haan Shoaib bhai, bataiye kya hukum hai? Main aapke phone ke sare commands lene ke liye taiyar hoon! Aap bolkar ya type karke WhatsApp, YouTube, Flashlight, Call, Settings ya Notifications control kar sakte hain.",
                    actionType = PhoneAction.TYPE_INFO,
                    status = "COMPLETED"
                )
            )

            // Seed initial phone notifications for Shoaib Akhtar
            repository.insertNotification(
                NotificationItemEntity(
                    appName = "WhatsApp",
                    title = "Shoaib bhai, meeting update",
                    message = "Project review scheduled for tomorrow at 11:00 AM",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 15
                )
            )
            repository.insertNotification(
                NotificationItemEntity(
                    appName = "Phone",
                    title = "Missed Call",
                    message = "+91 98765 43210 se call aaya tha",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60
                )
            )
        }
    }

    fun handleVoiceResult(speechText: String) {
        if (speechText.isNotBlank()) {
            processUserCommand(speechText)
        }
    }

    fun setListeningState(listening: Boolean) {
        _isListening.value = listening
    }

    fun setRms(rms: Float) {
        _rmsDb.value = rms
    }

    fun toggleHandsFreeMode() {
        val newState = !_isHandsFreeMode.value
        _isHandsFreeMode.value = newState
        voiceManager?.isHandsFreeMode = newState
        if (newState) {
            voiceManager?.speak("Hands-free mode on kar diya hai Shoaib bhai. Ab aap bina touch kiye baat kar sakte hain.")
        } else {
            voiceManager?.speak("Hands-free mode off kar diya hai Shoaib bhai.")
        }
    }

    fun toggleListening() {
        if (_isListening.value) {
            voiceManager?.stopListening()
            _isListening.value = false
        } else {
            voiceManager?.startListening()
        }
    }

    fun toggleTts() {
        val newState = !_isTtsEnabled.value
        _isTtsEnabled.value = newState
        voiceManager?.isTtsMuted = !newState
    }

    fun toggleExecutionMode() {
        _executionMode.value = if (_executionMode.value == ExecutionMode.SAFE_CONFIRMATION) {
            ExecutionMode.INSTANT_ACTION
        } else {
            ExecutionMode.SAFE_CONFIRMATION
        }
    }

    fun toggleNotificationSheet(show: Boolean) {
        _showNotificationSheet.value = show
    }

    fun triggerWakeAssistant() {
        processUserCommand("Ai ✨ Assistant")
    }

    fun processUserCommand(rawPrompt: String) {
        val trimmed = rawPrompt.trim()
        if (trimmed.isBlank() || _isProcessing.value) return

        viewModelScope.launch {
            _isProcessing.value = true

            // 1. Save Shoaib Akhtar's message
            repository.insertMessage(
                ChatMessageEntity(
                    sender = "Shoaib Akhtar",
                    text = trimmed,
                    timestamp = System.currentTimeMillis()
                )
            )

            // 2. Check if there is a pending confirmation that Shoaib is responding to by voice
            val pendingMsg = messages.value.findLast { it.status == "PENDING_CONFIRMATION" }
            if (pendingMsg != null) {
                val lower = trimmed.lowercase()
                val isAffirmative = lower.contains("haan") || lower.contains("yes") || lower.contains("confirm") ||
                        lower.contains("kardo") || lower.contains("theek hai") || lower.contains("approve") ||
                        lower.contains("bhejo") || lower == "ha" || lower.contains("bilkul")
                val isNegative = lower.contains("nahi") || lower.contains("no") || lower.contains("cancel") ||
                        lower.contains("mat karo") || lower.contains("ruk jao") || lower.contains("reject") || lower == "na"

                if (isAffirmative) {
                    confirmPendingAction(pendingMsg)
                    _isProcessing.value = false
                    return@launch
                } else if (isNegative) {
                    cancelPendingAction(pendingMsg)
                    _isProcessing.value = false
                    return@launch
                }
            }

            // 3. Parse command using Gemini or Local Engine
            val action = HinglishCommandParser.parseWithGemini(trimmed)

            val requiresConfirmation = action.isSensitive && _executionMode.value == ExecutionMode.SAFE_CONFIRMATION

            if (requiresConfirmation) {
                val confirmPrompt = "Shoaib bhai, yeh sensitive action hai (${action.type} - ${action.target}). Kya aap isko execute karna chahte hain?"
                repository.insertMessage(
                    ChatMessageEntity(
                        sender = "Ai ✨ Assistant",
                        text = confirmPrompt,
                        actionType = action.type,
                        actionTarget = action.target,
                        actionDetails = action.details,
                        isSensitive = true,
                        status = "PENDING_CONFIRMATION"
                    )
                )
                voiceManager?.speak(confirmPrompt)
            } else {
                // Execute immediately
                val result = actionExecutor.execute(action)
                val replyText = if (action.responseSpeech.isNotBlank()) action.responseSpeech else result.message

                repository.insertMessage(
                    ChatMessageEntity(
                        sender = "Ai ✨ Assistant",
                        text = replyText,
                        actionType = action.type,
                        actionTarget = action.target,
                        actionDetails = action.details,
                        isSensitive = action.isSensitive,
                        status = if (result.success) "EXECUTED" else "FAILED"
                    )
                )

                if (action.type == PhoneAction.TYPE_NOTIFICATIONS) {
                    _showNotificationSheet.value = true
                }

                voiceManager?.speak(replyText)
            }

            _isProcessing.value = false
        }
    }

    fun confirmPendingAction(msg: ChatMessageEntity) {
        viewModelScope.launch {
            val action = PhoneAction(
                type = msg.actionType ?: PhoneAction.TYPE_INFO,
                target = msg.actionTarget ?: "",
                details = msg.actionDetails ?: "",
                isSensitive = false
            )
            val result = actionExecutor.execute(action)
            val updatedMsg = msg.copy(
                text = "${msg.text}\n[Shoaib bhai dwara Approved ✓] ${result.message}",
                status = "EXECUTED"
            )
            repository.updateMessage(updatedMsg)
            voiceManager?.speak("Action confirm kar diya gaya hai Shoaib bhai.")
        }
    }

    fun cancelPendingAction(msg: ChatMessageEntity) {
        viewModelScope.launch {
            val updatedMsg = msg.copy(
                text = "${msg.text}\n[Shoaib bhai dwara Cancelled ✕]",
                status = "CANCELLED"
            )
            repository.updateMessage(updatedMsg)
            voiceManager?.speak("Action cancel kar diya gaya hai Shoaib bhai.")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearMessages()
            checkAndSeedInitialData()
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }
}
