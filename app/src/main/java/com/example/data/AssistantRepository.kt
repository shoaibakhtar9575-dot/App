package com.example.data

import kotlinx.coroutines.flow.Flow

class AssistantRepository(private val dao: AssistantDao) {
    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()
    val allNotifications: Flow<List<NotificationItemEntity>> = dao.getAllNotifications()

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        return dao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessageEntity) {
        dao.updateMessage(message)
    }

    suspend fun clearMessages() {
        dao.clearAllMessages()
    }

    suspend fun insertNotification(notification: NotificationItemEntity): Long {
        return dao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Long) {
        dao.markNotificationAsRead(id)
    }

    suspend fun clearNotifications() {
        dao.clearAllNotifications()
    }
}
