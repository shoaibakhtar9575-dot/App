package com.example.actions

data class PhoneAction(
    val type: String, // OPEN_APP, CALL, SMS, FLASHLIGHT, SETTINGS, ALARM, SEARCH, NOTIFICATIONS, INFO
    val target: String = "", // e.g. "whatsapp", "9876543210", "wifi", "calculator"
    val details: String = "", // e.g. "Message text or search query"
    val responseSpeech: String = "",
    val isSensitive: Boolean = false
) {
    companion object {
        const val TYPE_OPEN_APP = "OPEN_APP"
        const val TYPE_CALL = "CALL"
        const val TYPE_SMS = "SMS"
        const val TYPE_FLASHLIGHT = "FLASHLIGHT"
        const val TYPE_SETTINGS = "SETTINGS"
        const val TYPE_ALARM = "ALARM"
        const val TYPE_SEARCH = "SEARCH"
        const val TYPE_NOTIFICATIONS = "NOTIFICATIONS"
        const val TYPE_INFO = "INFO"
    }
}
