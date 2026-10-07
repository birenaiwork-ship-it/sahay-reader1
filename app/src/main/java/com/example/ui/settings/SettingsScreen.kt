package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.SahayaApplication
import com.example.data.remote.ApiClient
import com.example.ui.accessibility.AccessibilityHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SahayaApplication
    val prefsRepo = app.preferencesRepository
    val translationRepo = app.translationRepository
    val tts = app.ttsManager
    val scope = rememberCoroutineScope()

    val preferences by prefsRepo.preferencesFlow.collectAsState(initial = null)
    val isEnglishVoiceSupported by tts.isEnglishSupported.collectAsState()
    val isOdiaVoiceSupported by tts.isOdiaSupported.collectAsState()

    var showClearTranslationsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var backendUrlInput by remember { mutableStateOf("") }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<String?>(null) }

    // Synchronize backendUrlInput from preferences on load
    androidx.compose.runtime.LaunchedEffect(preferences?.backendBaseUrl) {
        preferences?.backendBaseUrl?.let {
            if (backendUrlInput.isEmpty()) {
                backendUrlInput = it
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Back to previous screen" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                .testTag("settings_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            preferences?.let { prefs ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section 1: Visual & Accessibility Preferences
                    Text(
                        text = "Visual & Accessibility",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )

                    SettingsToggleCard(
                        title = "High Contrast Mode",
                        subtitle = "WCAG AAA pure black background with vibrant yellow highlights",
                        isChecked = prefs.highContrast,
                        onCheckedChange = {
                            scope.launch { prefsRepo.setHighContrast(it) }
                            AccessibilityHelper.announce(context, if (it) "High contrast enabled" else "High contrast disabled")
                        }
                    )

                    SettingsToggleCard(
                        title = "Large Text Scale",
                        subtitle = "Increases reading font sizes by 25% across all screens",
                        isChecked = prefs.largeText,
                        onCheckedChange = {
                            scope.launch { prefsRepo.setLargeText(it) }
                            AccessibilityHelper.announce(context, if (it) "Large text enabled" else "Large text disabled")
                        }
                    )

                    SettingsToggleCard(
                        title = "Haptic Vibration Feedback",
                        subtitle = "Vibrates lightly on key reading events and bookmark toggles",
                        isChecked = prefs.hapticFeedback,
                        onCheckedChange = { scope.launch { prefsRepo.setHapticFeedback(it) } }
                    )

                    SettingsToggleCard(
                        title = "Keep Screen Awake While Reading",
                        subtitle = "Prevents device display from sleeping during book playback",
                        isChecked = prefs.keepScreenAwake,
                        onCheckedChange = { scope.launch { prefsRepo.setKeepScreenAwake(it) } }
                    )

                    // Section 2: Speech & Voice Settings
                    Text(
                        text = "Speech & Voice",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )

                    // English Speech Rate Slider
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Speech Speed",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${prefs.speechRateEnglish}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = prefs.speechRateEnglish,
                                onValueChange = { speed ->
                                    val rounded = (Math.round(speed * 4) / 4.0f).coerceIn(0.5f, 2.0f)
                                    scope.launch { prefsRepo.setSpeechRateEnglish(rounded) }
                                },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.semantics {
                                    contentDescription = "Speech speed slider, currently ${prefs.speechRateEnglish}x"
                                }
                            )
                        }
                    }

                    // Voice Engine Diagnostic Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Installed TTS Voice Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• English Voice: ${if (isEnglishVoiceSupported) "Installed & Ready" else "Missing data"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "• Odia Local Voice: ${if (isOdiaVoiceSupported) "Installed locally" else "Not installed (Cloud voice will be used)"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Section 3: Bilingual Reading Settings
                    Text(
                        text = "Bilingual Configuration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )

                    // Pause Duration
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Pause Between English and Odia",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0.5f, 1.0f, 2.0f).forEach { sec ->
                                    FilterChip(
                                        selected = prefs.bilingualPauseSec == sec,
                                        onClick = { scope.launch { prefsRepo.setBilingualPauseSec(sec) } },
                                        label = { Text("${sec}s") }
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Backend API Configuration
                    Text(
                        text = "Cloud Translation & TTS Server",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Backend Service Base URL",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "For local testing use http://10.0.2.2:8080/ (Android Emulator). Cloud secrets are managed securely on the server.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = backendUrlInput,
                                onValueChange = { backendUrlInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Base URL") },
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            prefsRepo.setBackendBaseUrl(backendUrlInput.trim())
                                            AccessibilityHelper.announce(context, "Server URL saved")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save URL")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isTestingConnection = true
                                            connectionTestResult = null
                                            try {
                                                val client = ApiClient.createService(backendUrlInput.trim())
                                                val response = client.checkHealth()
                                                connectionTestResult = if (response.isSuccessful) {
                                                    "Server connection successful! Status: ${response.body()?.status}"
                                                } else {
                                                    "Server error code: ${response.code()}"
                                                }
                                            } catch (e: Exception) {
                                                connectionTestResult = "Failed to connect: ${e.localizedMessage}"
                                            } finally {
                                                isTestingConnection = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isTestingConnection) "Testing…" else "Test")
                                }
                            }

                            connectionTestResult?.let { res ->
                                Text(
                                    text = res,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (res.startsWith("Server connection successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Section 5: Data & Privacy Actions
                    Text(
                        text = "Data Management & Privacy",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )

                    OutlinedButton(
                        onClick = { showClearTranslationsDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Cached Translations")
                    }

                    OutlinedButton(
                        onClick = {
                            val cacheDir = context.cacheDir
                            val deletedCount = cacheDir.listFiles()?.count { it.delete() } ?: 0
                            AccessibilityHelper.announce(context, "Cleaned $deletedCount temporary files")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clean Temporary Files")
                    }

                    Button(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Information")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Clear Translations Confirmation Dialog
    if (showClearTranslationsDialog) {
        AlertDialog(
            onDismissRequest = { showClearTranslationsDialog = false },
            title = { Text("Clear Cached Translations") },
            text = { Text("This will delete all saved Odia translations from local storage. Next time you read, they will be re-translated via the server.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            translationRepo.clearAllTranslations()
                            AccessibilityHelper.announce(context, "All cached translations cleared")
                            showClearTranslationsDialog = false
                        }
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTranslationsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Data Policy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• All textbook PDFs and extracted reading units are stored locally on your device.")
                    Text("• When you request Odia translation, only the active English reading unit text is securely transmitted to your configured backend server.")
                    Text("• No cloud API keys or personal identification credentials are stored within the Android application.")
                    Text("• You can delete books, audiobooks, or cached translations completely at any time using 'Delete Book' or 'Clear Cached Translations'.")
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Switch,
                onClickLabel = if (isChecked) "Turn off $title" else "Turn on $title",
                onClick = { onCheckedChange(!isChecked) }
            )
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                contentDescription = "$title. $subtitle. State: ${if (isChecked) "On" else "Off"}."
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = null
            )
        }
    }
}
