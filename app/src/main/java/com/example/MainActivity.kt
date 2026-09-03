package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.AssistantScreen
import com.example.ui.AssistantViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.voice.VoiceAssistantManager

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()
    private var voiceManager: VoiceAssistantManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        voiceManager = VoiceAssistantManager(
            context = this,
            onSpeechResult = { text ->
                viewModel.handleVoiceResult(text)
            },
            onListeningStateChanged = { listening ->
                viewModel.setListeningState(listening)
            },
            onRmsChanged = { rms ->
                viewModel.setRms(rms)
            }
        )
        viewModel.voiceManager = voiceManager

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
                    if (audioGranted && voiceManager?.isHandsFreeMode == true) {
                        voiceManager?.startListening()
                    }
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    val hasAudio = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (!hasAudio) {
                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.CAMERA)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    } else if (voiceManager?.isHandsFreeMode == true) {
                        voiceManager?.startListening()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        voiceManager?.stopSpeaking()
                        voiceManager?.stopListening()
                    }
                }

                AssistantScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.destroy()
        voiceManager = null
    }
}

