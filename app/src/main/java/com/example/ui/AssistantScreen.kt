package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.actions.PhoneAction
import com.example.data.ChatMessageEntity
import com.example.data.NotificationItemEntity
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDark
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val executionMode by viewModel.executionMode.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isHandsFreeMode by viewModel.isHandsFreeMode.collectAsStateWithLifecycle()
    val rmsDb by viewModel.rmsDb.collectAsStateWithLifecycle()
    val showNotificationSheet by viewModel.showNotificationSheet.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("assistant_screen"),
        containerColor = DarkBg,
        topBar = {
            TopAssistantBar(
                statusBarPadding = statusBarPadding,
                executionMode = executionMode,
                isTtsEnabled = isTtsEnabled,
                isHandsFreeMode = isHandsFreeMode,
                notificationCount = notifications.size,
                onToggleMode = { viewModel.toggleExecutionMode() },
                onToggleTts = { viewModel.toggleTts() },
                onToggleHandsFree = { viewModel.toggleHandsFreeMode() },
                onOpenNotifications = { viewModel.toggleNotificationSheet(true) },
                onClearChat = { viewModel.clearHistory() }
            )
        },
        bottomBar = {
            BottomInputBar(
                navBarPadding = navBarPadding,
                inputText = inputText,
                isListening = isListening,
                isProcessing = isProcessing,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.processUserCommand(inputText)
                    inputText = ""
                },
                onToggleListening = { viewModel.toggleListening() },
                onQuickAction = { prompt ->
                    viewModel.processUserCommand(prompt)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("conversation_history_list"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
            ) {
                // Header Banner Card
                item {
                    AssistantStatusBanner(
                        executionMode = executionMode,
                        onWakeAssistant = { viewModel.triggerWakeAssistant() }
                    )
                }

                // Chat Messages Feed
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onConfirmAction = { viewModel.confirmPendingAction(msg) },
                        onCancelAction = { viewModel.cancelPendingAction(msg) }
                    )
                }

                if (isProcessing) {
                    item {
                        ThinkingBubble()
                    }
                }
            }

            // Listening Audio Wave Overlay
            AnimatedVisibility(
                visible = isListening,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                ListeningBanner(
                    onStop = { viewModel.toggleListening() },
                    isHandsFree = isHandsFreeMode,
                    rmsDb = rmsDb
                )
            }
        }
    }

    if (showNotificationSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleNotificationSheet(false) },
            sheetState = sheetState,
            containerColor = DarkSurfaceElevated
        ) {
            NotificationsHubSheet(
                notifications = notifications,
                onMarkRead = { viewModel.markNotificationRead(it) },
                onClearAll = { viewModel.clearAllNotifications() },
                onClose = { viewModel.toggleNotificationSheet(false) }
            )
        }
    }
}

@Composable
fun TopAssistantBar(
    statusBarPadding: PaddingValues,
    executionMode: ExecutionMode,
    isTtsEnabled: Boolean,
    isHandsFreeMode: Boolean,
    notificationCount: Int,
    onToggleMode: () -> Unit,
    onToggleTts: () -> Unit,
    onToggleHandsFree: () -> Unit,
    onOpenNotifications: () -> Unit,
    onClearChat: () -> Unit
) {
    Surface(
        color = DarkSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Assistant Title & Shoaib Akhtar User Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, CyberCyan, CircleShape)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_ai_avatar),
                            contentDescription = "Ai Assistant Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ai ✨ Assistant",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                        }
                        Text(
                            text = "Shoaib Akhtar's Phone Master",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Control Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Hands-Free Quick Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isHandsFreeMode) Color(0xFF064E3B) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHandsFreeMode) EmeraldSuccess else Color(0xFF334155)),
                        modifier = Modifier
                            .clickable { onToggleHandsFree() }
                            .testTag("hands_free_header_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isHandsFreeMode) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (isHandsFreeMode) EmeraldSuccess else Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHandsFreeMode) "Hands-Free" else "Manual",
                                color = if (isHandsFreeMode) Color.White else Color.Gray,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // TTS Voice Toggle
                    IconButton(
                        onClick = onToggleTts,
                        modifier = Modifier.testTag("toggle_tts_button")
                    ) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Toggle Assistant Voice",
                            tint = if (isTtsEnabled) CyberCyan else Color.Gray
                        )
                    }

                    // Notifications Hub
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("notifications_hub_button")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Permitted Notifications",
                                tint = if (notificationCount > 0) NeonPurple else Color.LightGray
                            )
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(AmberAlert)
                                 )
                            }
                        }
                    }

                    // Clear Chat
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Reset Conversation",
                            tint = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mode Selector Pill & Voice Hands-Free Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (executionMode == ExecutionMode.SAFE_CONFIRMATION) DarkSurfaceVariant else Color(0xFF1E293B),
                    modifier = Modifier
                        .clickable { onToggleMode() }
                        .testTag("execution_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (executionMode == ExecutionMode.SAFE_CONFIRMATION) Icons.Default.Security else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (executionMode == ExecutionMode.SAFE_CONFIRMATION) CyberCyan else AmberAlert,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (executionMode == ExecutionMode.SAFE_CONFIRMATION) "🛡️ Safe Guard Mode" else "⚡ Instant Action",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = if (isHandsFreeMode) "🎙️ SpeechRecognizer Active" else "Hinglish Voice & Text",
                    color = if (isHandsFreeMode) EmeraldSuccess else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = if (isHandsFreeMode) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun AssistantStatusBanner(
    executionMode: ExecutionMode,
    onWakeAssistant: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A364F)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assistant_status_banner")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone Access & Assistant Status",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick test button for wake phrase
                Button(
                    onClick = onWakeAssistant,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("wake_assistant_button")
                ) {
                    Text("Ai ✨ Assistant", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Shoaib Akhtar ke phone me calls, SMS, apps aur settings safely manage ho rahe hain. ${if (executionMode == ExecutionMode.SAFE_CONFIRMATION) "Sensitive actions se pehle aapse confirmation manga jayega." else "Instant mode chalu hai: Commands turant execute honge."}",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit
) {
    val isUser = message.sender == "Shoaib Akhtar"
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_message_${message.id}" else "assistant_message_${message.id}"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (isUser) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0369A1),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "SA",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Surface(
                    shape = CircleShape,
                    color = CyberCyan,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✨",
                            fontSize = 9.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = if (isUser) "Shoaib Akhtar" else "Ai ✨ Assistant",
                color = if (isUser) CyberCyan else NeonPurple,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeStr,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        // Message Body
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            color = if (isUser) Color(0xFF0284C7) else DarkSurfaceElevated,
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243048)) else null,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp
                )

                // Interactive Confirmation Card for Sensitive Actions
                if (message.status == "PENDING_CONFIRMATION" && message.isSensitive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberAlert),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Confirmation Needed",
                                    tint = AmberAlert,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sensitive Action Detected: ${message.actionType}",
                                    color = AmberAlert,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!message.actionTarget.isNullOrBlank()) {
                                Text(
                                    text = "Target: ${message.actionTarget}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onConfirmAction,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_action_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirm", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = onCancelAction,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cancel_action_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                } else if (message.status == "EXECUTED" && message.actionType != null && message.actionType != PhoneAction.TYPE_INFO) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Executed",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Phone Action Executed: ${message.actionType}",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingBubble() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        CircularProgressIndicator(
            color = CyberCyan,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Ai ✨ Assistant soch raha hai...",
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ListeningBanner(
    onStop: () -> Unit,
    isHandsFree: Boolean,
    rmsDb: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "listening_pulse"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isHandsFree) EmeraldSuccess else CyberCyan),
        shadowElevation = 10.dp,
        modifier = Modifier
            .clickable { onStop() }
            .testTag("listening_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (isHandsFree) EmeraldSuccess else CyberCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Listening",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHandsFree) "🎙️ Hands-Free Listening..." else "Boliye Shoaib bhai...",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 4 bouncing audio equalizer bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val baseHeight = (rmsDb.coerceIn(0f, 10f) * 2f + 6f).dp
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(baseHeight * 0.8f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CyberCyan)
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(baseHeight * 1.3f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(EmeraldSuccess)
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(baseHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(NeonPurple)
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(baseHeight * 0.6f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CyberCyan)
                        )
                    }
                }
                Text(
                    text = if (isHandsFree) "Shoaib bhai, bina phone touch kiye boliye! (Tap to pause)" else "Hinglish voice command sun raha hoon (Tap to stop)",
                    color = if (isHandsFree) EmeraldSuccess else CyberCyan,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun BottomInputBar(
    navBarPadding: PaddingValues,
    inputText: String,
    isListening: Boolean,
    isProcessing: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleListening: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    Surface(
        color = DarkSurface,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBarPadding.calculateBottomPadding())
        ) {
            // Quick Action Chips in Hinglish
            val quickChips = listOf(
                "Ai ✨ Assistant" to Icons.Default.Security,
                "WhatsApp kholo" to Icons.Default.Phone,
                "Flashlight on karo" to Icons.Default.FlashOn,
                "YouTube chalao" to Icons.Default.Bolt,
                "Camera open karo" to Icons.Default.Security,
                "Wi-Fi Settings" to Icons.Default.Settings,
                "Notifications check karo" to Icons.Default.Notifications
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickChips) { (chipText, chipIcon) ->
                    FilterChip(
                        selected = false,
                        onClick = { onQuickAction(chipText) },
                        label = { Text(chipText, fontSize = 11.5.sp, color = Color.White) },
                        leadingIcon = {
                            Icon(chipIcon, contentDescription = null, modifier = Modifier.size(13.dp), tint = CyberCyan)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurfaceElevated,
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF2E3D59),
                            enabled = true,
                            selected = false
                        ),
                        modifier = Modifier.testTag("quick_chip_${chipText.replace(" ", "_")}")
                    )
                }
            }

            // Input Row: Text Field + Glowing Voice Mic + Send
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text("Shoaib bhai, yahan command likhein...", color = Color.Gray, fontSize = 13.5.sp)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Voice Mic Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (isListening) Brush.linearGradient(listOf(Color.Red, NeonPurple))
                            else Brush.linearGradient(listOf(CyberCyan, CyberCyanDark))
                        )
                        .clickable { onToggleListening() }
                        .testTag("mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Command",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) NeonPurple else Color(0xFF1E293B))
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Command",
                        tint = if (inputText.isNotBlank()) Color.White else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsHubSheet(
    notifications: List<NotificationItemEntity>,
    onMarkRead: (Long) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("notifications_hub_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shoaib Akhtar's Notifications",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (notifications.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    color = AmberAlert,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onClearAll() }
                        .padding(4.dp)
                        .testTag("clear_notifications_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sirf aapke phone ki permitted notifications yahan securely save ki gayi hain (No external tracking).",
            color = Color.Gray,
            fontSize = 11.5.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Koi nayi notification nahi hai Shoaib bhai.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243048)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMarkRead(notif.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = notif.appName.take(1).uppercase(),
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = notif.appName,
                                        color = CyberCyan,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(notif.timestamp)),
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = notif.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = notif.message,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("close_notifications_button")
        ) {
            Text("Close", color = Color.White)
        }
    }
}
