package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_notifications")
data class NotificationItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
