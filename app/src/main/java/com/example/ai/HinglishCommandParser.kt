package com.example.ai

import com.example.actions.PhoneAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object HinglishCommandParser {

    private const val SYSTEM_PROMPT = """
You are 'Shoaib Akhtar's Personal AI Assistant' (identifying with and representing Shoaib Akhtar, mobile phone personal assistant).
CRITICAL IDENTITY & BEHAVIORAL RULES:
1. IDENTITY & LOYALTY: You are Shoaib Akhtar's devoted and obedient mobile assistant ("Shoaib bhai aapka har hukum sar aankhon par"). You execute every voice command obediently and accurately.
2. ALL-ROUND KNOWLEDGE: You have deep, comprehensive knowledge on every subject (technology, science, smartphones, cricket, history, daily life assistance, math, and facts). Explain clearly, intelligently, and helpfully.
3. LANGUAGE: Communicate exclusively and naturally in crisp, friendly Hinglish (Hindi written in clean Latin/Roman script mixed with natural English terms).
4. STRICT GREETING RESTRICTION: EXPLICITLY NEVER say, write, or generate the word "Namaste", "Namaskar", or "Pranam" under any circumstances! Instead, greet naturally with "Haan Shoaib bhai", "Salam Shoaib bhai", "Hey Shoaib", "Boliye Shoaib bhai", or get straight to the point.
5. TONE: Warm, confident, respectful, loyal, and energetic.
6. PHONE AUTOMATION: For mobile phone commands, always respond with a valid JSON object:
{
  "action": "OPEN_APP" | "CALL" | "SMS" | "FLASHLIGHT" | "SETTINGS" | "ALARM" | "SEARCH" | "NOTIFICATIONS" | "INFO",
  "target": "string (app name like instagram/hotspot/spotify, contact/number, setting type)",
  "details": "string (message body, search query, alarm time, on/off state)",
  "isSensitive": true/false (true for CALL, SMS, settings change, file/data delete),
  "reply": "string (Crisp, conversational Hinglish response explicitly without 'Namaste')"
}
7. Supported Actions:
   - "hotspot off/on" -> action: "SETTINGS", target: "hotspot", details: "off" or "on"
   - "open <any app>" -> action: "OPEN_APP", target: "<app_name>"
   - "flashlight on/off" -> action: "FLASHLIGHT"
   - General queries -> action: "INFO", with full, informative Hinglish reply.
"""

    /**
     * Fast local rule-based parsing for zero-latency, offline, and deterministic command execution.
     */
    fun parseLocally(rawText: String): PhoneAction? {
        val trimmed = rawText.trim()
        val lower = trimmed.lowercase()

        // 1. Wake word / Assistant greeting trigger
        if (lower.contains("ai ✨ assistant") || lower == "ai assistant" || lower == "assistant" ||
            lower == "hey assistant" || lower == "hello assistant" || lower == "sunona" || lower == "suno"
        ) {
            return PhoneAction(
                type = PhoneAction.TYPE_INFO,
                target = "",
                details = "",
                responseSpeech = "Haan Shoaib bhai, bataiye kya hukum hai? Main taiyar hoon! Phone me kya action karna hai?",
                isSensitive = false
            )
        }

        // 2. Obedience & Capability / Knowledge queries
        if (lower.contains("meri saari baat") || lower.contains("meri baat mane") || lower.contains("meri baat mano") ||
            lower.contains("sab jankari") || lower.contains("har baat mano") || lower.contains("meri sari baat") ||
            lower.contains("kya tum meri baat")
        ) {
            return PhoneAction(
                type = PhoneAction.TYPE_INFO,
                target = "",
                details = "",
                responseSpeech = "Ji Shoaib bhai! Main aapki har ek baat manunga. Hotspot off ya on karna ho, koi bhi app open karni ho, flashlight, call, alarm ya duniya ki koi bhi jankari—aap bas boliye, sab turant execute hoga!",
                isSensitive = false
            )
        }

        // 3. Hotspot / Tethering Voice Control
        if (lower.contains("hotspot") || lower.contains("tether")) {
            val state = if (lower.contains("off") || lower.contains("band") || lower.contains("bujhao")) "off" else "on"
            return PhoneAction(
                type = PhoneAction.TYPE_SETTINGS,
                target = "hotspot",
                details = state,
                responseSpeech = "Shoaib bhai, Hotspot & Tethering settings open kar raha hoon jahan se aap ise turant $state kar sakte hain!",
                isSensitive = false
            )
        }

        // 4. Flashlight
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("flash light")) {
            val state = when {
                lower.contains("off") || lower.contains("band") || lower.contains("bujhao") -> "off"
                lower.contains("on") || lower.contains("chalu") || lower.contains("jalao") -> "on"
                else -> "toggle"
            }
            return PhoneAction(
                type = PhoneAction.TYPE_FLASHLIGHT,
                target = state,
                details = "",
                responseSpeech = if (state == "off") "Flashlight band kar di hai Shoaib bhai." else "Flashlight on kar di hai Shoaib bhai!",
                isSensitive = false
            )
        }

        // 5. WhatsApp
        if (lower.contains("whatsapp")) {
            val isMsg = lower.contains("message") || lower.contains("bhejo") || lower.contains("send")
            val phone = Regex("\\+?\\d{10,13}").find(lower)?.value ?: ""
            return PhoneAction(
                type = PhoneAction.TYPE_OPEN_APP,
                target = "whatsapp",
                details = phone,
                responseSpeech = if (isMsg) "WhatsApp message ready kar raha hoon Shoaib bhai." else "WhatsApp open kar raha hoon Shoaib bhai.",
                isSensitive = isMsg
            )
        }

        // 6. YouTube
        if (lower.contains("youtube") || lower.contains("video") || lower.contains("gaana") || lower.contains("song")) {
            val query = trimmed.replace(Regex("(?i)(youtube|pe|par|kholo|chalao|play|search|open|video|gaana|song)"), "").trim()
            return if (query.isNotBlank() && (lower.contains("chalao") || lower.contains("play") || lower.contains("search"))) {
                PhoneAction(
                    type = PhoneAction.TYPE_SEARCH,
                    target = "youtube",
                    details = query,
                    responseSpeech = "YouTube par '$query' chala raha hoon Shoaib bhai!",
                    isSensitive = false
                )
            } else {
                PhoneAction(
                    type = PhoneAction.TYPE_OPEN_APP,
                    target = "youtube",
                    details = "",
                    responseSpeech = "YouTube open kar diya hai Shoaib bhai.",
                    isSensitive = false
                )
            }
        }

        // 7. Camera
        if (lower.contains("camera") || lower.contains("photo khincho") || lower.contains("selfie")) {
            return PhoneAction(
                type = PhoneAction.TYPE_OPEN_APP,
                target = "camera",
                details = "",
                responseSpeech = "Camera open ho gaya hai Shoaib bhai. Smile please!",
                isSensitive = false
            )
        }

        // 8. Call
        if (lower.contains("call") || lower.contains("dial") || lower.contains("phone milao") || lower.contains("call karo")) {
            val number = Regex("\\+?\\d{3,14}").find(trimmed)?.value ?: ""
            val name = if (number.isBlank()) {
                trimmed.replace(Regex("(?i)(call|karo|ko|dial|phone|milao)"), "").trim()
            } else ""
            return PhoneAction(
                type = PhoneAction.TYPE_CALL,
                target = number.ifBlank { name.ifBlank { "Dialer" } },
                details = "Call request",
                responseSpeech = "Call connect karne ke liye confirmation taiyar hai Shoaib bhai.",
                isSensitive = true
            )
        }

        // 9. SMS / Message
        if (lower.contains("sms") || (lower.contains("message") && !lower.contains("whatsapp"))) {
            val number = Regex("\\+?\\d{3,14}").find(trimmed)?.value ?: ""
            return PhoneAction(
                type = PhoneAction.TYPE_SMS,
                target = number,
                details = "Ai Assistant message",
                responseSpeech = "SMS draft tayyar hai Shoaib bhai. Confirm kijiye.",
                isSensitive = true
            )
        }

        // 10. Specific App Names Voice Launcher
        val knownApps = listOf(
            "instagram", "telegram", "spotify", "snapchat", "facebook", "twitter",
            "netflix", "prime", "paytm", "phonepe", "gpay", "google pay", "zomato",
            "swiggy", "amazon", "flipkart", "chrome", "gmail", "gallery", "files"
        )
        for (appName in knownApps) {
            if (lower.contains(appName)) {
                return PhoneAction(
                    type = PhoneAction.TYPE_OPEN_APP,
                    target = appName,
                    details = "",
                    responseSpeech = "$appName open kar raha hoon Shoaib bhai!",
                    isSensitive = false
                )
            }
        }

        // 11. Generic App Opening Commands (e.g. "open [any app]", "[any app] open kar", "[any app] kholo")
        val openPrefixRegex = Regex("(?i)^(open|kholo|chalao|start|launch)\\s+([a-zA-Z0-9 ]+)")
        val openSuffixRegex = Regex("(?i)^([a-zA-Z0-9 ]+)\\s+(open kar|open karo|kholo|chalao|start kar|start karo)$")

        val prefixMatch = openPrefixRegex.find(trimmed)
        val suffixMatch = openSuffixRegex.find(trimmed)

        val targetApp = when {
            prefixMatch != null -> prefixMatch.groupValues[2].trim()
            suffixMatch != null -> suffixMatch.groupValues[1].trim()
            else -> null
        }

        if (!targetApp.isNullOrBlank() && !targetApp.equals("hotspot", ignoreCase = true) && !targetApp.equals("flashlight", ignoreCase = true) && !targetApp.equals("alarm", ignoreCase = true)) {
            return PhoneAction(
                type = PhoneAction.TYPE_OPEN_APP,
                target = targetApp,
                details = "",
                responseSpeech = "$targetApp open kar raha hoon Shoaib bhai!",
                isSensitive = false
            )
        }

        // 12. Settings & Connectivity
        if (lower.contains("setting") || lower.contains("wifi") || lower.contains("wi-fi") ||
            lower.contains("bluetooth") || lower.contains("display") || lower.contains("volume") || lower.contains("awaz")
        ) {
            val target = when {
                lower.contains("wifi") || lower.contains("wi-fi") -> "wifi"
                lower.contains("bluetooth") -> "bluetooth"
                lower.contains("display") || lower.contains("brightness") -> "display"
                lower.contains("volume") || lower.contains("sound") || lower.contains("awaz") -> "sound"
                else -> "main"
            }
            return PhoneAction(
                type = PhoneAction.TYPE_SETTINGS,
                target = target,
                details = "",
                responseSpeech = "$target settings khol di hai Shoaib bhai.",
                isSensitive = false
            )
        }

        // 13. Calculator
        if (lower.contains("calculator") || lower.contains("hisab") || lower.contains("calculate")) {
            return PhoneAction(
                type = PhoneAction.TYPE_OPEN_APP,
                target = "calculator",
                details = "",
                responseSpeech = "Calculator open kar diya hai Shoaib bhai.",
                isSensitive = false
            )
        }

        // 14. Alarm
        if (lower.contains("alarm") || lower.contains("jaga dena") || lower.contains("timer")) {
            return PhoneAction(
                type = PhoneAction.TYPE_ALARM,
                target = trimmed,
                details = "Assistant Reminder",
                responseSpeech = "Alarm set kar diya hai Shoaib bhai.",
                isSensitive = false
            )
        }

        // 15. Notifications
        if (lower.contains("notification") || lower.contains("notif") || lower.contains("suchna") || lower.contains("updates")) {
            return PhoneAction(
                type = PhoneAction.TYPE_NOTIFICATIONS,
                target = "inbox",
                details = "",
                responseSpeech = "Shoaib bhai, aapki permitted notifications load kar di gayi hain.",
                isSensitive = false
            )
        }

        // 16. Search
        if (lower.contains("search") || lower.contains("google") || lower.contains("dhoondo")) {
            val query = trimmed.replace(Regex("(?i)(google|pe|par|search|karo|dhoondo)"), "").trim()
            return PhoneAction(
                type = PhoneAction.TYPE_SEARCH,
                target = "google",
                details = query.ifBlank { "Latest news" },
                responseSpeech = "'$query' search kiya ja raha hai Shoaib bhai.",
                isSensitive = false
            )
        }

        return null
    }

    /**
     * Calls Gemini API if API key is available, falls back gracefully to smart parser.
     */
    suspend fun parseWithGemini(userPrompt: String): PhoneAction = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()

        // Check local rule first for common phone commands
        val localMatch = parseLocally(userPrompt)

        // If local parser matched a specific high-confidence phone action (like flashlight, camera, wake word), return it immediately!
        if (localMatch != null && (localMatch.type != PhoneAction.TYPE_INFO || userPrompt.contains("Ai ✨ Assistant", ignoreCase = true))) {
            return@withContext localMatch
        }

        // If API key is missing or blank, use local fallback
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext localMatch ?: PhoneAction(
                type = PhoneAction.TYPE_INFO,
                target = "",
                details = "",
                responseSpeech = "Shoaib bhai, maine aapka message suna: \"$userPrompt\". Main aapke commands jaise WhatsApp, YouTube, Flashlight, Call, Settings or Notifications control karne ke liye taiyar hoon!",
                isSensitive = false
            )
        }

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = "User Shoaib Akhtar said: \"$userPrompt\". Respond according to system prompt."))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.3f, maxOutputTokens = 350)
            )

            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            // Attempt to parse JSON from model response
            val cleanJson = responseText.substringAfter("{").substringBeforeLast("}")
            if (cleanJson.isNotBlank()) {
                val fullJsonStr = "{$cleanJson}"
                val json = JSONObject(fullJsonStr)
                val action = json.optString("action", PhoneAction.TYPE_INFO)
                val target = json.optString("target", "")
                val details = json.optString("details", "")
                val isSensitive = json.optBoolean("isSensitive", action == PhoneAction.TYPE_CALL || action == PhoneAction.TYPE_SMS)
                val reply = json.optString("reply", "").sanitizeNoNamaste()

                return@withContext PhoneAction(
                    type = action,
                    target = target,
                    details = details,
                    responseSpeech = if (reply.isNotBlank()) reply else "Bilkul Shoaib bhai, kaam ho raha hai.",
                    isSensitive = isSensitive
                )
            } else {
                val replyText = responseText.trim().sanitizeNoNamaste()
                return@withContext PhoneAction(
                    type = PhoneAction.TYPE_INFO,
                    target = "",
                    details = "",
                    responseSpeech = replyText.ifBlank { "Ji Shoaib bhai, main sun raha hoon. Kya command dena chahenge?" },
                    isSensitive = false
                )
            }
        } catch (e: Exception) {
            // Graceful fallback to local engine
            localMatch ?: PhoneAction(
                type = PhoneAction.TYPE_INFO,
                target = "",
                details = "",
                responseSpeech = "Haan Shoaib bhai, main taiyar hoon. Aap bolkar ya type karke command de sakte hain jaise 'WhatsApp kholo', 'Flashlight on karo' ya 'Call karo'.",
                isSensitive = false
            )
        }
    }

    private fun String.sanitizeNoNamaste(): String {
        return this.replace(Regex("(?i)\\bnamaste[e]*\\b"), "Haan")
            .replace(Regex("(?i)\\bnamaskar[a]*\\b"), "Haan")
            .replace(Regex("(?i)\\bpranam\\b"), "Haan")
    }
}
