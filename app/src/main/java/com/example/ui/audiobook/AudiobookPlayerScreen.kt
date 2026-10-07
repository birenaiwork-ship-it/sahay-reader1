package com.example.ui.audiobook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.SahayaApplication
import com.example.audio.AudiobookPlayerManager
import com.example.ui.accessibility.AccessibilityHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookPlayerScreen(
    audiobookId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SahayaApplication
    val audiobookRepo = app.audiobookRepository
    val scope = rememberCoroutineScope()

    val playerManager = remember { AudiobookPlayerManager(context) }
    val playerState by playerManager.playerState.collectAsState()

    val audiobook by audiobookRepo.getAudiobookByIdFlow(audiobookId).collectAsState(initial = null)

    var showResumeDialog by remember { mutableStateOf(false) }
    var hasHandledInitialPosition by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.release()
        }
    }

    LaunchedEffect(audiobook) {
        audiobook?.let { ab ->
            if (!hasHandledInitialPosition && ab.filePath != null) {
                if (ab.lastPlaybackPositionMs > 5000L) {
                    showResumeDialog = true
                } else {
                    playerManager.loadAudiobook(ab.filePath, ab.lastPlaybackPositionMs) { pos ->
                        scope.launch { audiobookRepo.updatePlaybackPosition(ab.id, pos) }
                    }
                }
                hasHandledInitialPosition = true
            }
        }
    }

    BackHandler {
        audiobook?.let { ab ->
            scope.launch { audiobookRepo.updatePlaybackPosition(ab.id, playerState.currentPositionMs) }
        }
        playerManager.stop()
        onNavigateBack()
    }

    fun formatTime(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Audio Player",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            audiobook?.let { ab ->
                                scope.launch { audiobookRepo.updatePlaybackPosition(ab.id, playerState.currentPositionMs) }
                            }
                            playerManager.stop()
                            onNavigateBack()
                        },
                        modifier = Modifier.semantics { contentDescription = "Back to Audiobooks" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier.semantics {
                                contentDescription = "Playback speed ${playerState.playbackSpeed}x"
                            }
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        playerManager.setPlaybackSpeed(speed)
                                        AccessibilityHelper.announce(context, "Speed $speed times")
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("audiobook_player_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .size(160.dp)
                            .semantics { contentDescription = "Audiobook album art icon" },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = audiobook?.title ?: "Loading…",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${audiobook?.languageMode ?: ""} Audiobook • ${audiobook?.totalPages ?: 0} Pages",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }

                // Slider & Time
                Column(modifier = Modifier.fillMaxWidth()) {
                    val currentPos = playerState.currentPositionMs
                    val totalDuration = playerState.durationMs.coerceAtLeast(1L)
                    val sliderValue = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                    Slider(
                        value = sliderValue,
                        onValueChange = { frac ->
                            val targetMs = (frac * totalDuration).toLong()
                            playerManager.seekTo(targetMs)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Progress: ${formatTime(currentPos)} of ${formatTime(totalDuration)}"
                            }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPos),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = formatTime(totalDuration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Media Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = {
                                playerManager.seekBackward10Seconds()
                                AccessibilityHelper.announce(context, "Rewound 10 seconds")
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .semantics { contentDescription = "Seek backward 10 seconds" }
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = null, modifier = Modifier.size(36.dp))
                        }

                        // Play / Pause
                        Surface(
                            onClick = {
                                playerManager.togglePlayPause()
                                AccessibilityHelper.announce(
                                    context,
                                    if (playerState.isPlaying) "Paused" else "Playing"
                                )
                            },
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("audio_play_pause_button")
                                .semantics {
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        // Forward 30s
                        IconButton(
                            onClick = {
                                playerManager.seekForward30Seconds()
                                AccessibilityHelper.announce(context, "Forward 30 seconds")
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .semantics { contentDescription = "Seek forward 30 seconds" }
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stop button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                playerManager.stop()
                                AccessibilityHelper.announce(context, "Playback stopped")
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .semantics { contentDescription = "Stop playback" }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Resume Prompt Dialog
    if (showResumeDialog) {
        val lastPos = audiobook?.lastPlaybackPositionMs ?: 0L
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("Resume Audio") },
            text = { Text("Resume from ${formatTime(lastPos)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        audiobook?.filePath?.let { path ->
                            playerManager.loadAudiobook(path, lastPos) { pos ->
                                scope.launch { audiobookRepo.updatePlaybackPosition(audiobookId, pos) }
                            }
                            playerManager.play()
                        }
                        showResumeDialog = false
                    }
                ) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        audiobook?.filePath?.let { path ->
                            playerManager.loadAudiobook(path, 0L) { pos ->
                                scope.launch { audiobookRepo.updatePlaybackPosition(audiobookId, pos) }
                            }
                            playerManager.play()
                        }
                        showResumeDialog = false
                    }
                ) {
                    Text("Start Over")
                }
            }
        )
    }
}
