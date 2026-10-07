package com.example.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SahayaApplication
import com.example.data.local.entity.ReadingUnitEntity
import com.example.data.local.entity.TranslationEntity
import com.example.ui.accessibility.AccessibilityHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlaybackStatus {
    STOPPED,
    PLAYING,
    PAUSED
}

enum class ReadingMode(val label: String) {
    ENGLISH("English"),
    ODIA("Odia"),
    BILINGUAL("Bilingual")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    initialPage: Int = 1,
    initialUnitId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: (bookId: String) -> Unit,
    onNavigateToAudiobook: (bookId: String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SahayaApplication
    val bookRepo = app.bookRepository
    val translationRepo = app.translationRepository
    val bookmarkRepo = app.bookmarkRepository
    val prefsRepo = app.preferencesRepository
    val tts = app.ttsManager
    val scope = rememberCoroutineScope()

    val book by bookRepo.getBookByIdFlow(bookId).collectAsState(initial = null)
    val preferences by prefsRepo.preferencesFlow.collectAsState(initial = null)

    var currentPageNumber by remember { mutableIntStateOf(initialPage) }
    val readingUnits by bookRepo.getReadingUnitsForPageFlow(bookId, currentPageNumber)
        .collectAsState(initial = emptyList())

    var selectedUnitIndex by remember { mutableIntStateOf(0) }
    var playbackStatus by remember { mutableStateOf(PlaybackStatus.STOPPED) }
    var readingMode by remember { mutableStateOf(ReadingMode.BILINGUAL) }
    var speechSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Keep screen awake while reading if preference is enabled
    DisposableEffect(preferences?.keepScreenAwake) {
        val window = (context as? Activity)?.window
        if (preferences?.keepScreenAwake == true) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            tts.stop()
        }
    }

    // Set initial speech speed & mode from preferences
    LaunchedEffect(preferences) {
        preferences?.let { prefs ->
            speechSpeed = prefs.speechRateEnglish
            readingMode = when (prefs.defaultReadingMode) {
                "ENGLISH" -> ReadingMode.ENGLISH
                "ODIA" -> ReadingMode.ODIA
                else -> ReadingMode.BILINGUAL
            }
        }
    }

    // Locate initialUnitId if passed
    LaunchedEffect(readingUnits, initialUnitId) {
        if (initialUnitId != null && readingUnits.isNotEmpty()) {
            val idx = readingUnits.indexOfFirst { it.id == initialUnitId }
            if (idx != -1) {
                selectedUnitIndex = idx
                listState.animateScrollToItem(idx)
            }
        }
    }

    // Save reading position in database whenever page or unit changes
    LaunchedEffect(currentPageNumber, selectedUnitIndex, readingUnits) {
        if (readingUnits.isNotEmpty() && selectedUnitIndex in readingUnits.indices) {
            val currentUnit = readingUnits[selectedUnitIndex]
            bookRepo.updateReadingPosition(bookId, currentPageNumber, currentUnit.id)
        }
    }

    // Handle Back Button
    BackHandler {
        tts.stop()
        onNavigateBack()
    }

    // Helper functions for reading flow
    fun speakCurrentUnit(autoAdvance: Boolean = true) {
        if (readingUnits.isEmpty() || selectedUnitIndex !in readingUnits.indices) {
            playbackStatus = PlaybackStatus.STOPPED
            return
        }

        val currentUnit = readingUnits[selectedUnitIndex]
        playbackStatus = PlaybackStatus.PLAYING

        scope.launch {
            // Ensure Odia translation is available if Odia or Bilingual mode
            var odiaText = ""
            if (readingMode != ReadingMode.ENGLISH) {
                val cached = translationRepo.getCachedTranslation(currentUnit.id)
                if (cached != null && cached.status == TranslationEntity.STATUS_TRANSLATED && cached.odiaText.isNotBlank()) {
                    odiaText = cached.odiaText
                } else {
                    val res = translationRepo.translateReadingUnit(currentUnit.id, bookId, currentUnit.englishText)
                    odiaText = res.getOrDefault("")
                }
            }

            fun onUnitCompleted() {
                if (playbackStatus == PlaybackStatus.PLAYING && autoAdvance) {
                    if (selectedUnitIndex < readingUnits.size - 1) {
                        selectedUnitIndex++
                        scope.launch {
                            listState.animateScrollToItem(selectedUnitIndex)
                            delay(100)
                            speakCurrentUnit(autoAdvance = true)
                        }
                    } else {
                        // Page finished! Check if there is next page
                        val totalPages = book?.pageCount ?: 1
                        if (currentPageNumber < totalPages) {
                            AccessibilityHelper.announce(context, "Page $currentPageNumber completed. Moving to page ${currentPageNumber + 1}.")
                            currentPageNumber++
                            selectedUnitIndex = 0
                            scope.launch {
                                delay(300)
                                speakCurrentUnit(autoAdvance = true)
                            }
                        } else {
                            playbackStatus = PlaybackStatus.STOPPED
                            AccessibilityHelper.announce(context, "You have reached the end of the book.")
                        }
                    }
                } else {
                    playbackStatus = PlaybackStatus.STOPPED
                }
            }

            when (readingMode) {
                ReadingMode.ENGLISH -> {
                    tts.speak(
                        text = currentUnit.englishText,
                        language = "en",
                        speedRate = speechSpeed,
                        onDone = { onUnitCompleted() },
                        onError = { errMsg ->
                            AccessibilityHelper.announce(context, errMsg)
                            playbackStatus = PlaybackStatus.STOPPED
                        }
                    )
                }

                ReadingMode.ODIA -> {
                    if (odiaText.isNotBlank()) {
                        tts.speak(
                            text = odiaText,
                            language = "or",
                            speedRate = speechSpeed,
                            onDone = { onUnitCompleted() },
                            onError = { errMsg ->
                                AccessibilityHelper.announce(context, errMsg)
                                playbackStatus = PlaybackStatus.STOPPED
                            }
                        )
                    } else {
                        AccessibilityHelper.announce(context, "Odia translation not available yet. Reading English.")
                        tts.speak(
                            text = currentUnit.englishText,
                            language = "en",
                            speedRate = speechSpeed,
                            onDone = { onUnitCompleted() }
                        )
                    }
                }

                ReadingMode.BILINGUAL -> {
                    val pauseMs = ((preferences?.bilingualPauseSec ?: 1.0f) * 1000).toLong()
                    val isEnglishFirst = preferences?.bilingualOrder != "OR_THEN_EN"

                    if (isEnglishFirst) {
                        tts.speak(
                            text = currentUnit.englishText,
                            language = "en",
                            speedRate = speechSpeed,
                            onDone = {
                                if (odiaText.isNotBlank()) {
                                    scope.launch {
                                        delay(pauseMs)
                                        if (playbackStatus == PlaybackStatus.PLAYING) {
                                            tts.speak(
                                                text = odiaText,
                                                language = "or",
                                                speedRate = speechSpeed,
                                                onDone = { onUnitCompleted() },
                                                onError = { onUnitCompleted() }
                                            )
                                        }
                                    }
                                } else {
                                    onUnitCompleted()
                                }
                            },
                            onError = { onUnitCompleted() }
                        )
                    } else {
                        // Odia first, then English
                        if (odiaText.isNotBlank()) {
                            tts.speak(
                                text = odiaText,
                                language = "or",
                                speedRate = speechSpeed,
                                onDone = {
                                    scope.launch {
                                        delay(pauseMs)
                                        if (playbackStatus == PlaybackStatus.PLAYING) {
                                            tts.speak(
                                                text = currentUnit.englishText,
                                                language = "en",
                                                speedRate = speechSpeed,
                                                onDone = { onUnitCompleted() },
                                                onError = { onUnitCompleted() }
                                            )
                                        }
                                    }
                                },
                                onError = { onUnitCompleted() }
                            )
                        } else {
                            tts.speak(
                                text = currentUnit.englishText,
                                language = "en",
                                speedRate = speechSpeed,
                                onDone = { onUnitCompleted() }
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopPlayback() {
        tts.stop()
        playbackStatus = PlaybackStatus.STOPPED
        AccessibilityHelper.announce(context, "Playback stopped")
    }

    fun togglePlayPause() {
        if (playbackStatus == PlaybackStatus.PLAYING) {
            tts.stop()
            playbackStatus = PlaybackStatus.PAUSED
            AccessibilityHelper.announce(context, "Paused")
        } else {
            speakCurrentUnit(autoAdvance = true)
        }
    }

    fun selectPreviousUnit() {
        if (selectedUnitIndex > 0) {
            selectedUnitIndex--
            scope.launch { listState.animateScrollToItem(selectedUnitIndex) }
            val unit = readingUnits.getOrNull(selectedUnitIndex)
            AccessibilityHelper.announce(
                context,
                "Reading unit ${selectedUnitIndex + 1} of ${readingUnits.size}. ${unit?.englishText ?: ""}"
            )
            if (playbackStatus == PlaybackStatus.PLAYING) speakCurrentUnit(autoAdvance = true)
        }
    }

    fun selectNextUnit() {
        if (selectedUnitIndex < readingUnits.size - 1) {
            selectedUnitIndex++
            scope.launch { listState.animateScrollToItem(selectedUnitIndex) }
            val unit = readingUnits.getOrNull(selectedUnitIndex)
            AccessibilityHelper.announce(
                context,
                "Reading unit ${selectedUnitIndex + 1} of ${readingUnits.size}. ${unit?.englishText ?: ""}"
            )
            if (playbackStatus == PlaybackStatus.PLAYING) speakCurrentUnit(autoAdvance = true)
        }
    }

    fun selectPreviousPage() {
        if (currentPageNumber > 1) {
            tts.stop()
            playbackStatus = PlaybackStatus.STOPPED
            currentPageNumber--
            selectedUnitIndex = 0
            AccessibilityHelper.announce(context, "Page $currentPageNumber")
        }
    }

    fun selectNextPage() {
        val total = book?.pageCount ?: 1
        if (currentPageNumber < total) {
            tts.stop()
            playbackStatus = PlaybackStatus.STOPPED
            currentPageNumber++
            selectedUnitIndex = 0
            AccessibilityHelper.announce(context, "Page $currentPageNumber")
        }
    }

    // Physical Keyboard Shortcuts
    Modifier
    val keyboardModifier = Modifier
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when (keyEvent.key) {
                    Key.Spacebar -> {
                        togglePlayPause()
                        true
                    }
                    Key.DirectionUp -> {
                        selectPreviousUnit()
                        true
                    }
                    Key.DirectionDown -> {
                        selectNextUnit()
                        true
                    }
                    Key.DirectionLeft -> {
                        selectPreviousPage()
                        true
                    }
                    Key.DirectionRight -> {
                        selectNextPage()
                        true
                    }
                    Key.T -> {
                        // Translate current unit
                        readingUnits.getOrNull(selectedUnitIndex)?.let { unit ->
                            scope.launch {
                                AccessibilityHelper.announce(context, "Translating unit ${selectedUnitIndex + 1}")
                                val res = translationRepo.translateReadingUnit(unit.id, bookId, unit.englishText, forceRefresh = true)
                                if (res.isSuccess) {
                                    AccessibilityHelper.announce(context, "Translation ready: ${res.getOrNull()}")
                                } else {
                                    AccessibilityHelper.announce(context, "Translation failed: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        }
                        true
                    }
                    Key.E -> {
                        readingMode = ReadingMode.ENGLISH
                        AccessibilityHelper.announce(context, "English mode selected")
                        speakCurrentUnit(autoAdvance = true)
                        true
                    }
                    Key.O -> {
                        readingMode = ReadingMode.ODIA
                        AccessibilityHelper.announce(context, "Odia mode selected")
                        speakCurrentUnit(autoAdvance = true)
                        true
                    }
                    Key.B -> {
                        readingMode = ReadingMode.BILINGUAL
                        AccessibilityHelper.announce(context, "Bilingual mode selected")
                        speakCurrentUnit(autoAdvance = true)
                        true
                    }
                    Key.R -> {
                        // Repeat current unit
                        speakCurrentUnit(autoAdvance = false)
                        true
                    }
                    Key.A -> {
                        onNavigateToAudiobook(bookId)
                        true
                    }
                    Key.K -> {
                        // Toggle bookmark
                        readingUnits.getOrNull(selectedUnitIndex)?.let { unit ->
                            scope.launch {
                                val added = bookmarkRepo.toggleBookmark(
                                    bookId = bookId,
                                    pageNumber = currentPageNumber,
                                    readingUnitId = unit.id,
                                    readingUnitSnippet = unit.englishText
                                )
                                AccessibilityHelper.announce(
                                    context,
                                    if (added) "Bookmark added for unit ${selectedUnitIndex + 1}" else "Bookmark removed"
                                )
                            }
                        }
                        true
                    }
                    Key.S -> {
                        stopPlayback()
                        true
                    }
                    Key.Plus, Key.Equals -> {
                        val newSpeed = (speechSpeed + 0.25f).coerceAtMost(2.0f)
                        speechSpeed = newSpeed
                        scope.launch { prefsRepo.setSpeechRateEnglish(newSpeed) }
                        AccessibilityHelper.announce(context, "Speed $speechSpeed times")
                        true
                    }
                    Key.Minus -> {
                        val newSpeed = (speechSpeed - 0.25f).coerceAtLeast(0.5f)
                        speechSpeed = newSpeed
                        scope.launch { prefsRepo.setSpeechRateEnglish(newSpeed) }
                        AccessibilityHelper.announce(context, "Speed $speechSpeed times")
                        true
                    }
                    Key.Escape -> {
                        stopPlayback()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }

    Scaffold(
        modifier = keyboardModifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.title ?: "Reader",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            modifier = Modifier.semantics { heading() }
                        )
                        val totalPages = book?.pageCount ?: 1
                        Text(
                            text = "Page $currentPageNumber of $totalPages • Unit ${if (readingUnits.isEmpty()) 0 else selectedUnitIndex + 1} of ${readingUnits.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            tts.stop()
                            onNavigateBack()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back to previous screen"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToSearch(bookId) },
                        modifier = Modifier.semantics {
                            contentDescription = "Search in book"
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }

                    // Bookmark button for current unit
                    IconButton(
                        onClick = {
                            readingUnits.getOrNull(selectedUnitIndex)?.let { unit ->
                                scope.launch {
                                    val added = bookmarkRepo.toggleBookmark(
                                        bookId = bookId,
                                        pageNumber = currentPageNumber,
                                        readingUnitId = unit.id,
                                        readingUnitSnippet = unit.englishText
                                    )
                                    AccessibilityHelper.announce(
                                        context,
                                        if (added) "Bookmark added on page $currentPageNumber" else "Bookmark removed"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Toggle bookmark for current unit"
                        }
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                    }

                    // Speed selector
                    Box {
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier.semantics {
                                contentDescription = "Reading speech speed, currently $speechSpeed x"
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
                                        speechSpeed = speed
                                        scope.launch { prefsRepo.setSpeechRateEnglish(speed) }
                                        AccessibilityHelper.announce(context, "Speech speed set to ${speed}x")
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Accessible playback & navigation control bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Reading Mode Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReadingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = readingMode == mode,
                                onClick = {
                                    readingMode = mode
                                    AccessibilityHelper.announce(context, "${mode.label} reading mode selected")
                                },
                                label = { Text(mode.label, style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.semantics {
                                    contentDescription = "${mode.label} reading mode"
                                }
                            )
                        }

                        // Status badge (Playing, Paused, Stopped)
                        Text(
                            text = playbackStatus.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when (playbackStatus) {
                                PlaybackStatus.PLAYING -> MaterialTheme.colorScheme.primary
                                PlaybackStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                                PlaybackStatus.STOPPED -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Playback state: ${playbackStatus.name}"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Playback Controls Row (>=48dp touch targets)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Page
                        IconButton(
                            onClick = { selectPreviousPage() },
                            enabled = currentPageNumber > 1,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Previous page" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(32.dp))
                        }

                        // Previous Unit
                        IconButton(
                            onClick = { selectPreviousUnit() },
                            enabled = selectedUnitIndex > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Previous reading unit" }
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(28.dp))
                        }

                        // Play / Pause (Large Primary Button)
                        Surface(
                            onClick = { togglePlayPause() },
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("play_pause_button")
                                .semantics {
                                    contentDescription = if (playbackStatus == PlaybackStatus.PLAYING) "Pause reading" else "Play reading"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playbackStatus == PlaybackStatus.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Stop
                        IconButton(
                            onClick = { stopPlayback() },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Stop reading" }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(32.dp))
                        }

                        // Next Unit
                        IconButton(
                            onClick = { selectNextUnit() },
                            enabled = selectedUnitIndex < readingUnits.size - 1,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Next reading unit" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(32.dp))
                        }

                        // Next Page
                        val totalPages = book?.pageCount ?: 1
                        IconButton(
                            onClick = { selectNextPage() },
                            enabled = currentPageNumber < totalPages,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Next page" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("reader_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            if (readingUnits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Page $currentPageNumber is empty or still indexing.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(readingUnits, key = { _, unit -> unit.id }) { index, unit ->
                        val isSelected = index == selectedUnitIndex
                        val translation by translationRepo.getTranslationFlow(unit.id).collectAsState(initial = null)

                        ReadingUnitCard(
                            unit = unit,
                            unitNumber = index + 1,
                            totalUnits = readingUnits.size,
                            isSelected = isSelected,
                            pageNumber = currentPageNumber,
                            translation = translation,
                            onSelect = {
                                selectedUnitIndex = index
                                AccessibilityHelper.announce(
                                    context,
                                    "Page $currentPageNumber. Reading unit ${index + 1} of ${readingUnits.size} selected. ${unit.englishText}"
                                )
                                if (playbackStatus == PlaybackStatus.PLAYING) {
                                    speakCurrentUnit(autoAdvance = true)
                                }
                            },
                            onTranslate = {
                                scope.launch {
                                    AccessibilityHelper.announce(context, "Translating unit ${index + 1}")
                                    val res = translationRepo.translateReadingUnit(unit.id, bookId, unit.englishText, forceRefresh = true)
                                    if (res.isSuccess) {
                                        AccessibilityHelper.announce(context, "Translation ready: ${res.getOrNull()}")
                                    } else {
                                        AccessibilityHelper.announce(context, "Translation failed: ${res.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            onSpeakEnglish = {
                                selectedUnitIndex = index
                                tts.speak(unit.englishText, "en", speechSpeed)
                            },
                            onSpeakOdia = {
                                selectedUnitIndex = index
                                val odiaText = translation?.odiaText ?: ""
                                if (odiaText.isNotBlank()) {
                                    tts.speak(odiaText, "or", speechSpeed)
                                } else {
                                    AccessibilityHelper.announce(context, "Please translate to Odia first")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingUnitCard(
    unit: ReadingUnitEntity,
    unitNumber: Int,
    totalUnits: Int,
    isSelected: Boolean,
    pageNumber: Int,
    translation: TranslationEntity?,
    onSelect: () -> Unit,
    onTranslate: () -> Unit,
    onSpeakEnglish: () -> Unit,
    onSpeakOdia: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val accessibilityDesc = remember(isSelected, unit.englishText, translation?.odiaText) {
        val transPart = if (translation?.status == TranslationEntity.STATUS_TRANSLATED && translation.odiaText.isNotBlank()) {
            "Odia translation: ${translation.odiaText}"
        } else {
            "Not translated"
        }
        "Page $pageNumber, Reading unit $unitNumber of $totalUnits${if (isSelected) ", selected" else ""}. English text: ${unit.englishText}. $transPart"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("reading_unit_${unit.id}")
            .semantics {
                contentDescription = accessibilityDesc
                customActions = listOf(
                    CustomAccessibilityAction("Select unit $unitNumber") {
                        onSelect()
                        true
                    },
                    CustomAccessibilityAction("Read English aloud") {
                        onSpeakEnglish()
                        true
                    },
                    if (translation?.status == TranslationEntity.STATUS_TRANSLATED && translation.odiaText.isNotBlank()) {
                        CustomAccessibilityAction("Read Odia aloud") {
                            onSpeakOdia()
                            true
                        }
                    } else {
                        CustomAccessibilityAction("Translate to Odia") {
                            onTranslate()
                            true
                        }
                    }
                )
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Unit header index badge (clickable to select)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Select unit $unitNumber",
                        onClick = onSelect
                    )
                    .semantics {
                        contentDescription = "Unit $unitNumber of $totalUnits${if (isSelected) ", currently selected" else ""}. Double tap to select."
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Unit $unitNumber of $totalUnits",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (unit.isHeading) {
                    Text(
                        text = "HEADING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // English Text
            Text(
                text = unit.englishText,
                style = if (unit.isHeading) {
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp)
                },
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Select and play unit $unitNumber in English",
                        onClick = {
                            onSelect()
                            onSpeakEnglish()
                        }
                    )
                    .semantics {
                        contentDescription = "English text: ${unit.englishText}. Double tap to read aloud."
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Odia Translation Section
            when (translation?.status) {
                TranslationEntity.STATUS_TRANSLATED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Odia (ଓଡ଼ିଆ):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = onSpeakOdia,
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        contentDescription = "Read Odia text for unit $unitNumber aloud"
                                    }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = translation.odiaText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 32.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Read Odia translation aloud",
                                    onClick = onSpeakOdia
                                )
                                .semantics {
                                    contentDescription = "Odia translation: ${translation.odiaText}. Double tap to read aloud."
                                }
                        )
                    }
                }

                TranslationEntity.STATUS_TRANSLATING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Translating unit $unitNumber to Odia…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TranslationEntity.STATUS_FAILED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Translation failed (Offline or network error)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onTranslate,
                            modifier = Modifier
                                .height(48.dp)
                                .semantics { contentDescription = "Retry Odia translation for unit $unitNumber" }
                        ) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    // Not translated yet
                    OutlinedButton(
                        onClick = onTranslate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics {
                                contentDescription = "Translate reading unit $unitNumber into Odia"
                            },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate to Odia", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}
