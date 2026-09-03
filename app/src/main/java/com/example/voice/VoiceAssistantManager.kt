package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onRmsChanged: ((Float) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    var isTtsMuted = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    var isHandsFreeMode = true
        set(value) {
            field = value
            if (!value) {
                stopListening()
            } else if (!isSpeaking() && !isListening) {
                startListening()
            }
        }

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(1.05f)
            tts?.setPitch(1.0f)
            isTtsReady = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    stopListening()
                }

                override fun onDone(utteranceId: String?) {
                    if (isHandsFreeMode) {
                        mainHandler.postDelayed({
                            if (isHandsFreeMode && !isSpeaking()) {
                                startListening()
                            }
                        }, 500)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (isHandsFreeMode) {
                        mainHandler.postDelayed({
                            if (isHandsFreeMode && !isSpeaking()) {
                                startListening()
                            }
                        }, 600)
                    }
                }
            })
        }
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                isListening = true
                                onListeningStateChanged(true)
                            }

                            override fun onBeginningOfSpeech() {
                                isListening = true
                                onListeningStateChanged(true)
                            }

                            override fun onRmsChanged(rmsdB: Float) {
                                onRmsChanged?.invoke(rmsdB)
                            }

                            override fun onBufferReceived(buffer: ByteArray?) {}

                            override fun onEndOfSpeech() {
                                isListening = false
                                onListeningStateChanged(false)
                            }

                            override fun onError(error: Int) {
                                isListening = false
                                onListeningStateChanged(false)
                                Log.w("VoiceManager", "Speech recognition error code: $error")
                                if (isHandsFreeMode && !isSpeaking()) {
                                    val delay = when (error) {
                                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1000L
                                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH -> 1200L
                                        else -> 2000L
                                    }
                                    mainHandler.postDelayed({
                                        if (isHandsFreeMode && !isSpeaking() && !isListening) {
                                            startListening()
                                        }
                                    }, delay)
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                isListening = false
                                onListeningStateChanged(false)
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    val speech = matches[0]
                                    onSpeechResult(speech)
                                } else if (isHandsFreeMode && !isSpeaking()) {
                                    mainHandler.postDelayed({
                                        if (isHandsFreeMode && !isSpeaking() && !isListening) {
                                            startListening()
                                        }
                                    }, 1000)
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {}

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceManager", "Failed to init SpeechRecognizer", e)
            }
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun startListening() {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                return@post
            }
            if (isSpeaking()) {
                return@post
            }
            try {
                speechRecognizer?.cancel()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye Shoaib bhai, main sun raha hoon...")
                }
                speechRecognizer?.startListening(intent)
                isListening = true
                onListeningStateChanged(true)
            } catch (e: Exception) {
                Log.e("VoiceManager", "Error starting listening", e)
                isListening = false
                onListeningStateChanged(false)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                // ignore
            }
            isListening = false
            onListeningStateChanged(false)
        }
    }

    fun speak(text: String) {
        if (isTtsMuted || !isTtsReady || text.isBlank()) return
        // Stop current listening while speaking so assistant doesn't hear itself
        stopListening()
        // Double check: strictly NO "namaste" or "namaskar" allowed!
        val sanitized = text
            .replace(Regex("(?i)\\bnamaste[e]*\\b"), "Haan")
            .replace(Regex("(?i)\\bnamaskar[a]*\\b"), "Haan")
            .replace(Regex("(?i)\\bpranam\\b"), "Haan")
        val utteranceId = "UTTERANCE_${System.currentTimeMillis()}"
        tts?.speak(sanitized, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
    }
}
