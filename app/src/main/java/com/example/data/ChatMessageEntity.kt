package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "Shoaib Akhtar" or "Ai ✨ Assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // OPEN_APP, CALL, SMS, FLASHLIGHT, SETTINGS, ALARM, SEARCH, NOTIFICATIONS, INFO
    val actionTarget: String? = null,
    val actionDetails: String? = null,
    val isSensitive: Boolean = false,
    val status: String = "COMPLETED" // COMPLETED, PENDING_CONFIRMATION, CANCELLED, EXECUTED
)
