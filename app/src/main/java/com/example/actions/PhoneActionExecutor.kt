package com.example.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

class PhoneActionExecutor(private val context: Context) {

    private var isTorchOn: Boolean = false

    fun execute(action: PhoneAction): ExecutionResult {
        return try {
            when (action.type) {
                PhoneAction.TYPE_OPEN_APP -> openApp(action.target)
                PhoneAction.TYPE_CALL -> makeCall(action.target)
                PhoneAction.TYPE_SMS -> sendSms(action.target, action.details)
                PhoneAction.TYPE_FLASHLIGHT -> toggleFlashlight(action.target)
                PhoneAction.TYPE_SETTINGS -> openSettings(action.target)
                PhoneAction.TYPE_ALARM -> setAlarm(action.target, action.details)
                PhoneAction.TYPE_SEARCH -> performSearch(action.details.ifBlank { action.target })
                PhoneAction.TYPE_NOTIFICATIONS -> ExecutionResult(true, "Notifications hub opened")
                else -> ExecutionResult(true, action.responseSpeech)
            }
        } catch (e: Exception) {
            Log.e("PhoneActionExecutor", "Action failed", e)
            ExecutionResult(false, "Action execute karne me problem aayi: ${e.localizedMessage}")
        }
    }

    private fun openApp(appKey: String): ExecutionResult {
        val lower = appKey.lowercase().trim()
        val pm = context.packageManager

        // 1. Direct hardware & core app intents
        if (lower.contains("camera")) {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return ExecutionResult(true, "Camera open kar diya hai Shoaib bhai!")
        }

        if (lower.contains("calculator") || lower.contains("calc") || lower.contains("hisab")) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALCULATOR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return ExecutionResult(true, "Calculator open ho gaya hai Shoaib bhai.")
            }
        }

        if (lower.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return ExecutionResult(true, "Settings khol diya hai.")
        }

        if (lower.contains("dialer") || lower.contains("phone") || lower.contains("call app")) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return ExecutionResult(true, "Phone dialer khol diya hai.")
        }

        if (lower.contains("gallery") || lower.contains("photo")) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return ExecutionResult(true, "Gallery open kar di hai Shoaib bhai!")
            }
        }

        if (lower.contains("file") || lower.contains("document") || lower.contains("folder")) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return ExecutionResult(true, "Files app open ho gayi hai Shoaib bhai!")
            }
        }

        // 2. Dynamic Package & App Search across ALL installed applications on user's device
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val installedApps = pm.queryIntentActivities(launcherIntent, 0)
            val matchedApp = installedApps.firstOrNull { ri ->
                val label = ri.loadLabel(pm).toString().lowercase()
                val pkgName = ri.activityInfo.packageName.lowercase()
                label == lower || label.contains(lower) || lower.contains(label) || pkgName.contains(lower)
            }

            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    val label = matchedApp.loadLabel(pm).toString()
                    return ExecutionResult(true, "$label open kar diya hai Shoaib bhai!")
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneActionExecutor", "Error querying installed apps", e)
        }

        // 3. Known popular app package mappings
        val knownPkg = when {
            lower.contains("whatsapp") -> "com.whatsapp"
            lower.contains("youtube") -> "com.google.android.youtube"
            lower.contains("chrome") || lower.contains("browser") -> "com.android.chrome"
            lower.contains("instagram") || lower.contains("insta") -> "com.instagram.android"
            lower.contains("telegram") -> "org.telegram.messenger"
            lower.contains("spotify") -> "com.spotify.music"
            lower.contains("snapchat") -> "com.snapchat.android"
            lower.contains("facebook") || lower.contains("fb") -> "com.facebook.katana"
            lower.contains("twitter") || lower.contains("tweet") || lower == "x" -> "com.twitter.android"
            lower.contains("netflix") -> "com.netflix.mediaclient"
            lower.contains("prime") -> "com.amazon.avod.thirdpartyclient"
            lower.contains("paytm") -> "net.one97.paytm"
            lower.contains("phonepe") -> "com.phonepe.app"
            lower.contains("gpay") || lower.contains("google pay") -> "com.google.android.apps.nbu.paisa.user"
            lower.contains("zomato") -> "com.application.zomato"
            lower.contains("swiggy") -> "in.swiggy.android"
            lower.contains("amazon") -> "in.amazon.mShop.android.shopping"
            lower.contains("flipkart") -> "com.flipkart.android"
            lower.contains("map") -> "com.google.android.apps.maps"
            lower.contains("gmail") || lower.contains("mail") -> "com.google.android.gm"
            else -> null
        }

        if (knownPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(knownPkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ExecutionResult(true, "$appKey open ho gaya hai Shoaib bhai!")
            }
        }

        // 4. Web fallbacks for popular services
        if (lower.contains("youtube")) {
            val ytWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(ytWeb)
            return ExecutionResult(true, "YouTube web me open kar diya hai Shoaib bhai.")
        }
        if (lower.contains("whatsapp")) {
            val waWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(waWeb)
            return ExecutionResult(true, "WhatsApp web open kar diya hai.")
        }
        if (lower.contains("instagram")) {
            val instaWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(instaWeb)
            return ExecutionResult(true, "Instagram web par khol diya hai.")
        }

        return ExecutionResult(false, "$appKey app device me installed nahi mili Shoaib bhai.")
    }

    private fun makeCall(phoneNumber: String): ExecutionResult {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        val finalNumber = if (cleanNumber.isNotBlank()) cleanNumber else "121"
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$finalNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ExecutionResult(true, "Number $finalNumber dialer me lagaya ja raha hai Shoaib bhai.")
    }

    private fun sendSms(phoneNumber: String, message: String): ExecutionResult {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        val uri = if (cleanNumber.isNotBlank()) Uri.parse("smsto:$cleanNumber") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ExecutionResult(true, "SMS draft taiyar kar diya hai Shoaib bhai.")
    }

    fun toggleFlashlight(commandState: String = ""): ExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                return ExecutionResult(false, "Device me flashlight hardware nahi mila.")
            }

            val targetState = when {
                commandState.contains("off", ignoreCase = true) || commandState.contains("band", ignoreCase = true) -> false
                commandState.contains("on", ignoreCase = true) || commandState.contains("chalu", ignoreCase = true) -> true
                else -> !isTorchOn
            }

            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            val stateText = if (targetState) "on" else "off"
            ExecutionResult(true, "Flashlight $stateText kar di hai Shoaib bhai!")
        } catch (e: Exception) {
            Log.e("PhoneActionExecutor", "Torch error", e)
            ExecutionResult(false, "Flashlight toggle nahi ho paya: ${e.localizedMessage}")
        }
    }

    private fun openSettings(settingType: String): ExecutionResult {
        val lower = settingType.lowercase()
        val isHotspot = lower.contains("hotspot") || lower.contains("tether") || lower.contains("portable")

        val intent = when {
            isHotspot -> {
                try {
                    val tetherIntent = Intent().apply {
                        setClassName("com.android.settings", "com.android.settings.TetherSettings")
                    }
                    if (tetherIntent.resolveActivity(context.packageManager) != null) {
                        tetherIntent
                    } else {
                        Intent("android.settings.TETHER_SETTINGS")
                    }
                } catch (e: Exception) {
                    Intent(Settings.ACTION_WIRELESS_SETTINGS)
                }
            }
            lower.contains("wifi") || lower.contains("wi-fi") -> Intent(Settings.ACTION_WIFI_SETTINGS)
            lower.contains("bluetooth") -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            lower.contains("sound") || lower.contains("volume") || lower.contains("awaz") -> Intent(Settings.ACTION_SOUND_SETTINGS)
            lower.contains("display") || lower.contains("brightness") -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            lower.contains("battery") -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            lower.contains("data") || lower.contains("network") -> Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            lower.contains("app") -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            if (isHotspot) {
                ExecutionResult(true, "Shoaib bhai, Hotspot & Tethering settings open kar diya hai, yahan se aap ise turant toggle kar sakte hain!")
            } else {
                ExecutionResult(true, "$settingType settings open kar di hai Shoaib bhai.")
            }
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(fallback)
            ExecutionResult(true, "Settings screen open kar di hai Shoaib bhai.")
        }
    }

    private fun setAlarm(timeOrTarget: String, label: String): ExecutionResult {
        // extract hour and minute if present
        val timeRegex = Regex("(\\d{1,2})[:.]?(\\d{2})?")
        val match = timeRegex.find(timeOrTarget)
        val hour = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
        val minute = match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "Ai Assistant Alarm" })
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ExecutionResult(true, "$hour:$minute ka Alarm set kar diya hai Shoaib bhai.")
        } catch (e: Exception) {
            val showAlarms = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(showAlarms)
            ExecutionResult(true, "Alarm screen khol di hai Shoaib bhai.")
        }
    }

    private fun performSearch(query: String): ExecutionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ExecutionResult(true, "'$query' search kar raha hoon Shoaib bhai.")
        } catch (e: Exception) {
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browser)
            ExecutionResult(true, "'$query' Google par search ho raha hai.")
        }
    }
}

data class ExecutionResult(
    val success: Boolean,
    val message: String
)
