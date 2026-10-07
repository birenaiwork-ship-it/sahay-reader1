package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.local.entity.BookEntity
import com.example.pdf.ExtractionProgress
import com.example.sample.SampleBookProvider
import com.example.ui.accessibility.AccessibilityHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun HomeScreen(
    onNavigateToReader: (bookId: String, page: Int, unitId: String?) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToAudiobooks: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SahayaApplication
    val bookRepo = app.bookRepository
    val pdfExtractor = app.pdfTextExtractor
    val scope = rememberCoroutineScope()

    val recentBook by bookRepo.getMostRecentBookFlow().collectAsState(initial = null)

    var isExtracting by remember { mutableStateOf(false) }
    var extractionProgressText by remember { mutableStateOf("Opening book…") }
    var extractionProgressPercent by remember { mutableStateOf(0) }
    var extractionError by remember { mutableStateOf<String?>(null) }

    // PDF Picker contract
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isExtracting = true
                extractionError = null
                val bookId = UUID.randomUUID().toString()

                // Derive title from uri or fallback
                var fileName = "Document_${System.currentTimeMillis() % 10000}"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex).removeSuffix(".pdf")
                        }
                    }
                } catch (_: Exception) {}

                AccessibilityHelper.announce(context, "Opening $fileName. Extracting text.")

                val result = pdfExtractor.extractFromUri(bookId, uri) { progress ->
                    when (progress) {
                        is ExtractionProgress.Status -> {
                            extractionProgressText = progress.message
                            extractionProgressPercent = progress.percent
                            if (progress.currentPage > 0 && progress.currentPage % 5 == 0) {
                                AccessibilityHelper.announce(context, "Page ${progress.currentPage} extracted")
                            }
                        }
                        is ExtractionProgress.Error -> {
                            extractionError = progress.message
                            AccessibilityHelper.announce(context, "Error: ${progress.message}")
                        }
                        is ExtractionProgress.Success -> {
                            // Handled below
                        }
                    }
                }

                if (result.isSuccess) {
                    val (pages, units) = result.getOrThrow()
                    val savedFile = File(context.filesDir, "books/${bookId}.pdf")
                    val bookEntity = BookEntity(
                        id = bookId,
                        title = fileName,
                        pageCount = pages.size,
                        currentPage = 1,
                        currentReadingUnitId = units.firstOrNull()?.id,
                        lastReadTimestamp = System.currentTimeMillis(),
                        isSampleBook = false,
                        sourceUri = savedFile.absolutePath,
                        fileSize = if (savedFile.exists()) savedFile.length() else 0L
                    )
                    bookRepo.saveBook(bookEntity, pages, units)
                    isExtracting = false
                    AccessibilityHelper.announce(context, "Book ready: $fileName with ${pages.size} pages. Opening reader.")
                    onNavigateToReader(bookId, 1, units.firstOrNull()?.id)
                } else {
                    isExtracting = false
                    extractionError = result.exceptionOrNull()?.localizedMessage ?: "Failed to process PDF"
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sahaya Reader",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Accessible Book Reader & Audiobooks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Accessibility instruction
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Accessibility instruction: Use TalkBack or keyboard navigation. Press Play to begin reading."
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Use TalkBack or keyboard navigation. Press Play to begin reading.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Continue Reading Card (if recent book exists)
            recentBook?.let { book ->
                RecentBookCard(
                    book = book,
                    onContinue = {
                        AccessibilityHelper.announce(context, "Resuming ${book.title} on page ${book.currentPage}")
                        onNavigateToReader(book.id, book.currentPage, book.currentReadingUnitId)
                    }
                )
            }

            Text(
                text = "Main Actions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() }
            )

            // 1. Upload PDF
            ActionBigButton(
                icon = Icons.Default.UploadFile,
                title = "Upload PDF",
                subtitle = "Import English textbook from storage or Google Drive",
                testTag = "action_upload_pdf",
                onClick = {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
            )

            // 2. Sample Book (Instant Demo)
            ActionBigButton(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Sample Textbook",
                subtitle = "Introduction to Computer Science (pre-loaded demo)",
                testTag = "action_sample_book",
                onClick = {
                    scope.launch {
                        SampleBookProvider.seedSampleBookIfNotPresent(context, bookRepo, app.translationRepository)
                        onNavigateToReader(SampleBookProvider.SAMPLE_BOOK_ID, 1, null)
                    }
                }
            )

            // 3. My Books
            ActionBigButton(
                icon = Icons.Default.CollectionsBookmark,
                title = "My Books",
                subtitle = "View and manage imported textbooks and bookmarks",
                testTag = "action_my_books",
                onClick = onNavigateToLibrary
            )

            // 4. Audiobooks
            ActionBigButton(
                icon = Icons.Default.Headphones,
                title = "Audiobooks",
                subtitle = "Listen to generated English, Odia, or bilingual audiobooks",
                testTag = "action_audiobooks",
                onClick = onNavigateToAudiobooks
            )

            // 5. Settings
            ActionBigButton(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "Speech speed, translation server, contrast, and voice options",
                testTag = "action_settings",
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Extraction in Progress Dialog
    if (isExtracting) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible while processing */ },
            title = {
                Text(
                    text = "Processing Book",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { extractionProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = extractionProgressText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics {
                            contentDescription = "Status: $extractionProgressText, $extractionProgressPercent percent complete"
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Error Dialog
    extractionError?.let { err ->
        AlertDialog(
            onDismissRequest = { extractionError = null },
            title = { Text("Processing Error") },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = { extractionError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun RecentBookCard(
    book: BookEntity,
    onContinue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_book_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
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
                        contentDescription = "Recent Reading: ${book.title}, page ${book.currentPage} of ${book.pageCount}."
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Reading",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Page ${book.currentPage} of ${book.pageCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_reading_button")
                    .semantics {
                        contentDescription = "Continue reading ${book.title}, page ${book.currentPage}"
                    },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue reading ${book.title}, page ${book.currentPage}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ActionBigButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title. $subtitle"
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
