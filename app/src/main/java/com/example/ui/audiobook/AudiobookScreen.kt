package com.example.ui.audiobook

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.SahayaApplication
import com.example.data.local.entity.AudiobookEntity
import com.example.data.local.entity.BookEntity
import com.example.ui.accessibility.AccessibilityHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookScreen(
    onNavigateBack: () -> Unit,
    onPlayAudiobook: (audiobookId: String) -> Unit,
    preselectedBookId: String? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as SahayaApplication
    val audiobookRepo = app.audiobookRepository
    val bookRepo = app.bookRepository
    val scope = rememberCoroutineScope()

    val audiobooks by audiobookRepo.getAllAudiobooksFlow().collectAsState(initial = emptyList())
    val allBooks by bookRepo.getAllBooks().collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(preselectedBookId != null) }
    var selectedBookForCreation by remember {
        mutableStateOf(allBooks.find { it.id == preselectedBookId } ?: allBooks.firstOrNull())
    }
    var selectedMode by remember { mutableStateOf("ENGLISH") }
    var audiobookToDelete by remember { mutableStateOf<AudiobookEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Audiobooks",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (allBooks.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        selectedBookForCreation = allBooks.find { it.id == preselectedBookId } ?: allBooks.firstOrNull()
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .testTag("create_audiobook_fab")
                        .semantics {
                            contentDescription = "Create new audiobook"
                        }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("audiobooks_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            if (audiobooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No audiobooks generated yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (allBooks.isNotEmpty()) {
                            Button(
                                onClick = {
                                    selectedBookForCreation = allBooks.firstOrNull()
                                    showCreateDialog = true
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Create Audiobook from Book")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(audiobooks, key = { it.id }) { item ->
                        AudiobookCard(
                            audiobook = item,
                            onPlay = {
                                if (item.status == AudiobookEntity.STATUS_COMPLETED) {
                                    onPlayAudiobook(item.id)
                                } else {
                                    AccessibilityHelper.announce(context, "Audiobook is still generating")
                                }
                            },
                            onDelete = {
                                audiobookToDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Audiobook Dialog
    if (showCreateDialog && allBooks.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "What would you like to generate?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Book: ${selectedBookForCreation?.title ?: "Select Book"}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    listOf(
                        Pair("ENGLISH", "English audiobook"),
                        Pair("ODIA", "Odia audiobook (auto-translates)"),
                        Pair("BILINGUAL", "Bilingual audiobook (English + Odia)")
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedMode == mode,
                                    role = Role.RadioButton,
                                    onClick = { selectedMode = mode }
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val book = selectedBookForCreation
                        if (book != null) {
                            scope.launch {
                                val units = bookRepo.getAllReadingUnitsForBook(book.id)
                                val id = audiobookRepo.createAndStartAudiobookGeneration(
                                    bookId = book.id,
                                    bookTitle = book.title,
                                    languageMode = selectedMode,
                                    totalPages = book.pageCount,
                                    totalUnits = units.size
                                )
                                AccessibilityHelper.announce(context, "Audiobook generation started for ${book.title}")
                                showCreateDialog = false
                            }
                        }
                    }
                ) {
                    Text("Start Generating")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    audiobookToDelete?.let { audiobook ->
        AlertDialog(
            onDismissRequest = { audiobookToDelete = null },
            title = { Text("Delete Audiobook") },
            text = { Text("Are you sure you want to delete '${audiobook.title}' audiobook?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            audiobookRepo.deleteAudiobook(audiobook.id)
                            AccessibilityHelper.announce(context, "Deleted audiobook")
                            audiobookToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { audiobookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AudiobookCard(
    audiobook: AudiobookEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(audiobook.createdAt) {
        dateFormatter.format(Date(audiobook.createdAt))
    }

    val durationText = remember(audiobook.totalDurationMs) {
        val totalSecs = audiobook.totalDurationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        "${mins}m ${secs}s"
    }

    val accessibilityDesc = remember(audiobook) {
        when (audiobook.status) {
            AudiobookEntity.STATUS_COMPLETED ->
                "Audiobook: ${audiobook.title}, ${audiobook.languageMode} mode. Duration $durationText, completed."
            AudiobookEntity.STATUS_PROCESSING ->
                "Audiobook: ${audiobook.title}, generating. Progress ${audiobook.progressPercent} percent, page ${audiobook.currentProcessingPage} of ${audiobook.totalPages}."
            else ->
                "Audiobook: ${audiobook.title}, status ${audiobook.status}."
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audiobook_card_${audiobook.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = accessibilityDesc
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${audiobook.languageMode} Audiobook",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = audiobook.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (audiobook.status) {
                AudiobookEntity.STATUS_PROCESSING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { audiobook.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Page ${audiobook.currentProcessingPage} of ${audiobook.totalPages} • ${audiobook.progressPercent}% complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AudiobookEntity.STATUS_COMPLETED -> {
                    Text(
                        text = "Duration: $durationText • Format: ${audiobook.fileFormat}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPlay,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .semantics {
                                    contentDescription = "Play audiobook ${audiobook.title}"
                                },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Audiobook")
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Delete audiobook ${audiobook.title}" }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                AudiobookEntity.STATUS_FAILED -> {
                    Text(
                        text = "Failed: ${audiobook.errorMessage ?: "Unknown error"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete failed audiobook")
                    }
                }

                else -> {
                    Text(text = "Queued in background…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
