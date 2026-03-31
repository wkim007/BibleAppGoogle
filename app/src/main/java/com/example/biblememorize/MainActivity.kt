package com.example.biblememorize

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import com.example.biblememorize.ui.theme.BibleMemorizeTheme
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Verse(
    val id: String,
    val reference: String,
    val translation: String,
    val text: String,
    val prompt: String
)

private data class BibleBook(
    val name: String,
    val chapterCount: Int,
    val knownVerseCounts: Map<Int, Int> = emptyMap()
)

private enum class VerseContentType(val label: String) {
    DueNow("Due Now"),
    Upcoming("Upcoming")
}

private enum class VerseDifficulty(val label: String) {
    Easy("Easy"),
    Medium("Medium"),
    Hard("Hard")
}

private val starterVerses = listOf(
    Verse(
        id = "joshua-1-9",
        reference = "Joshua 1:9",
        translation = "NKJV",
        text = "Have I not commanded you? Be strong and of good courage; do not be afraid, nor be dismayed, for the Lord your God is with you wherever you go.",
        prompt = "Courage comes with God's presence."
    ),
    Verse(
        id = "psalm-119-11",
        reference = "Psalm 119:11",
        translation = "NIV",
        text = "I have hidden your word in my heart that I might not sin against you.",
        prompt = "Hide Scripture in your heart."
    ),
    Verse(
        id = "philippians-4-6",
        reference = "Philippians 4:6",
        translation = "NIV",
        text = "Do not be anxious about anything, but in every situation, by prayer and petition, with thanksgiving, present your requests to God.",
        prompt = "Trade anxiety for prayer."
    )
)

private val bibleVersions = listOf("KJV", "NKJV", "개역한글", "中文", "Español", "日本語", "Deutsch")
private val bibleBooks = listOf(
    BibleBook("Genesis", 50, mapOf(1 to 31)),
    BibleBook("Exodus", 40), BibleBook("Leviticus", 27), BibleBook("Numbers", 36),
    BibleBook("Deuteronomy", 34), BibleBook("Joshua", 24), BibleBook("Judges", 21),
    BibleBook("Ruth", 4), BibleBook("1 Samuel", 31), BibleBook("2 Samuel", 24),
    BibleBook("1 Kings", 22), BibleBook("2 Kings", 25), BibleBook("1 Chronicles", 29),
    BibleBook("2 Chronicles", 36), BibleBook("Ezra", 10), BibleBook("Nehemiah", 13),
    BibleBook("Esther", 10), BibleBook("Job", 42), BibleBook("Psalms", 150),
    BibleBook("Proverbs", 31), BibleBook("Ecclesiastes", 12), BibleBook("Song of Solomon", 8),
    BibleBook("Isaiah", 66), BibleBook("Jeremiah", 52), BibleBook("Lamentations", 5),
    BibleBook("Ezekiel", 48), BibleBook("Daniel", 12), BibleBook("Hosea", 14),
    BibleBook("Joel", 3), BibleBook("Amos", 9), BibleBook("Obadiah", 1),
    BibleBook("Jonah", 4), BibleBook("Micah", 7), BibleBook("Nahum", 3),
    BibleBook("Habakkuk", 3), BibleBook("Zephaniah", 3), BibleBook("Haggai", 2),
    BibleBook("Zechariah", 14), BibleBook("Malachi", 4), BibleBook("Matthew", 28),
    BibleBook("Mark", 16), BibleBook("Luke", 24), BibleBook("John", 21, mapOf(1 to 51)),
    BibleBook("Acts", 28), BibleBook("Romans", 16), BibleBook("1 Corinthians", 16),
    BibleBook("2 Corinthians", 13), BibleBook("Galatians", 6), BibleBook("Ephesians", 6),
    BibleBook("Philippians", 4), BibleBook("Colossians", 4), BibleBook("1 Thessalonians", 5),
    BibleBook("2 Thessalonians", 3), BibleBook("1 Timothy", 6), BibleBook("2 Timothy", 4),
    BibleBook("Titus", 3), BibleBook("Philemon", 1), BibleBook("Hebrews", 13),
    BibleBook("James", 5), BibleBook("1 Peter", 5), BibleBook("2 Peter", 3),
    BibleBook("1 John", 5), BibleBook("2 John", 1), BibleBook("3 John", 1),
    BibleBook("Jude", 1), BibleBook("Revelation", 22)
)

private enum class BottomTab(val label: String) {
    Review("Review"),
    Progress("Progress"),
    Settings("Settings")
}

private enum class RepeatMode(val badge: String) {
    Off(""),
    Once("1"),
    Twice("2"),
    Infinite("∞");

    fun next(): RepeatMode = when (this) {
        Off -> Once
        Once -> Twice
        Twice -> Infinite
        Infinite -> Off
    }
}

private enum class VerseSection {
    DueNow,
    Upcoming
}

private data class DragState(
    val verse: Verse,
    val fromSection: VerseSection,
    val position: Offset
)

private data class PendingDelete(
    val verse: Verse,
    val section: VerseSection
)

private data class PendingEdit(
    val verse: Verse,
    val section: VerseSection
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BibleMemorizeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MemorizeApp()
                }
            }
        }
    }
}

@Composable
private fun MemorizeApp() {
    val context = LocalContext.current
    var hiddenWordCount by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableStateOf(BottomTab.Review) }
    var selectedBibleVersion by remember { mutableStateOf("NKJV") }
    var speechRate by remember { mutableStateOf(0.9f) }
    var showAddVersePage by remember { mutableStateOf(false) }
    var isReviewing by remember { mutableStateOf(false) }
    var showReviewAnswer by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(RepeatMode.Off) }
    var dueRepeatModes by remember { mutableStateOf<Map<String, RepeatMode>>(emptyMap()) }
    var upcomingRepeatModes by remember { mutableStateOf<Map<String, RepeatMode>>(emptyMap()) }
    var recognizedText by remember { mutableStateOf("") }
    var matchedIndices by remember { mutableStateOf(setOf<Int>()) }
    var reviewCompleted by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var pendingReviewCompletion by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }
    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
    var voiceLevel by remember { mutableStateOf(0f) }
    val persistedVerseState = remember(context) { loadPersistedVerseState(context) }
    var versePassCounts by remember(context) { mutableStateOf(loadPersistedVersePassCounts(context)) }
    val dueVerses = remember {
        mutableStateListOf(*persistedVerseState.first.toTypedArray())
    }
    val upcomingVerses = remember {
        mutableStateListOf(*persistedVerseState.second.toTypedArray())
    }
    val verse = dueVerses.firstOrNull() ?: upcomingVerses.firstOrNull() ?: starterVerses.first()
    val passCount = versePassCounts.values.sum()
    val verseWords = remember(verse.id, verse.reference, verse.text) { verse.text.split(" ") }
    val reviewHiddenIndices = remember(verse.id, verse.reference, verse.text) { buildReviewHiddenIndices(verseWords) }
    val dueCount = dueVerses.size
    val reviewScope = rememberCoroutineScope()
    val speaker = rememberVerseSpeaker(speechRate)
    val voiceRecognizer = rememberVoiceRecognizer(
        onResult = { spokenText ->
            val mergedText = mergeRecognizedText(recognizedText, spokenText)
            recognizedText = mergedText
            val newMatchedIndices = matchRecognizedWords(
                verseWords = verseWords,
                hiddenIndices = reviewHiddenIndices,
                recognizedText = mergedText
            )
            matchedIndices = newMatchedIndices
            if (reviewHiddenIndices.isNotEmpty() && reviewHiddenIndices.all { it in newMatchedIndices }) {
                pendingReviewCompletion = true
            }
        },
        onError = {
            voiceLevel = 0f
        },
        onListeningStateChanged = { _ -> },
        onLevelChanged = { voiceLevel = it }
    )
    val resetReviewSession = {
        showReviewAnswer = false
        recognizedText = ""
        matchedIndices = emptySet()
        reviewCompleted = false
        showCompletionDialog = false
        pendingReviewCompletion = false
        voiceLevel = 0f
        repeatMode = RepeatMode.Off
        voiceRecognizer.disableAutoRestart()
        voiceRecognizer.stopListening()
    }
    val progress by animateFloatAsState(
        targetValue = if (verseWords.isEmpty()) 0f else hiddenWordCount.toFloat() / verseWords.size.toFloat(),
        label = "memorizeProgress"
    )
    val reviewFullyMatched = reviewHiddenIndices.isNotEmpty() && reviewHiddenIndices.all { it in matchedIndices }
    val displayProgressPercent = if (reviewCompleted || reviewFullyMatched) 100 else (progress * 100).roundToInt()
    val microphonePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecognizer.startListening()
        } else {
            recognizedText = "Microphone permission is required."
        }
    }

    LaunchedEffect(verse.id, isReviewing) {
        showReviewAnswer = false
        recognizedText = ""
        matchedIndices = emptySet()
        reviewCompleted = false
        showCompletionDialog = false
        pendingReviewCompletion = false
        voiceLevel = 0f
        if (!isReviewing) {
            repeatMode = RepeatMode.Off
            speaker.stop()
            voiceRecognizer.stopListening()
        }
    }

    LaunchedEffect(pendingReviewCompletion) {
        if (pendingReviewCompletion && !reviewCompleted) {
            reviewCompleted = true
            versePassCounts = versePassCounts + (verse.id to ((versePassCounts[verse.id] ?: 0) + 1))
            recognizedText = ""
            voiceRecognizer.disableAutoRestart()
            voiceRecognizer.stopListening()
            showCompletionDialog = true
            pendingReviewCompletion = false
        }
    }

    LaunchedEffect(dueVerses, upcomingVerses, context) {
        snapshotFlow {
            Pair(dueVerses.toList(), upcomingVerses.toList())
        }.collectLatest { (savedDueVerses, savedUpcomingVerses) ->
            savePersistedVerseState(context, savedDueVerses, savedUpcomingVerses)
        }
    }

    LaunchedEffect(context) {
        snapshotFlow { versePassCounts }
            .collectLatest { savePersistedVersePassCounts(context, it) }
    }

    val moveVerseBetweenSections: (Verse, VerseSection, VerseSection) -> Unit = { draggedVerse, from, to ->
        if (from != to) {
            val fromList = if (from == VerseSection.DueNow) dueVerses else upcomingVerses
            val toList = if (to == VerseSection.DueNow) dueVerses else upcomingVerses
            val removeIndex = fromList.indexOfFirst { it.id == draggedVerse.id }
            if (removeIndex >= 0) {
                val verseToMove = fromList.removeAt(removeIndex)
                if (to == VerseSection.DueNow) {
                    toList.add(0, verseToMove)
                } else {
                    toList.add(verseToMove)
                }
                hiddenWordCount = 0
                reviewCompleted = false
                pendingReviewCompletion = false
                recognizedText = ""
                matchedIndices = emptySet()
                voiceRecognizer.disableAutoRestart()
                voiceRecognizer.stopListening()
            }
        }
    }

    val applyVerseEdit: (PendingEdit, Verse, VerseContentType) -> Unit = { editState, updatedVerse, contentType ->
        val sourceList = if (editState.section == VerseSection.DueNow) dueVerses else upcomingVerses
        val targetSection = if (contentType == VerseContentType.DueNow) VerseSection.DueNow else VerseSection.Upcoming
        val targetList = if (targetSection == VerseSection.DueNow) dueVerses else upcomingVerses
        val sourceIndex = sourceList.indexOfFirst { it.id == editState.verse.id }

        if (sourceIndex >= 0) {
            sourceList.removeAt(sourceIndex)
            if (targetSection == editState.section) {
                sourceList.add(sourceIndex.coerceAtMost(sourceList.size), updatedVerse)
            } else if (targetSection == VerseSection.DueNow) {
                targetList.add(0, updatedVerse)
            } else {
                targetList.add(updatedVerse)
            }
        }

        if (verse.id == editState.verse.id) {
            hiddenWordCount = 0
            resetReviewSession()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                BottomTab.Review -> {
                    ReviewScreen(
                        innerPadding = innerPadding,
                        verse = verse,
                        dueCount = dueCount,
                        passCount = passCount,
                        progressPercent = displayProgressPercent,
                        onResetReview = resetReviewSession,
                        dueVerses = dueVerses,
                        upcomingVerses = upcomingVerses,
                        dueRepeatModes = dueRepeatModes,
                        upcomingRepeatModes = upcomingRepeatModes,
                        versePassCounts = versePassCounts,
                        activeDueLooping = speaker.isLooping,
                        onMoveVerse = moveVerseBetweenSections,
                        onAddVerse = { showAddVersePage = true },
                        onPlay = {
                            val totalWords = verseWords.size
                            hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(totalWords)
                            isReviewing = true
                        },
                        onSpeak = { selectedVerse ->
                            speaker.speak(
                                selectedVerse,
                                dueRepeatModes[selectedVerse.id] ?: RepeatMode.Off
                            )
                        },
                        onRepeatToggle = { selectedVerse ->
                            val currentMode = dueRepeatModes[selectedVerse.id] ?: RepeatMode.Off
                            dueRepeatModes = dueRepeatModes + (selectedVerse.id to currentMode.next())
                        },
                        onUpcomingSpeak = { selectedVerse ->
                            speaker.speak(
                                selectedVerse,
                                upcomingRepeatModes[selectedVerse.id] ?: RepeatMode.Off
                            )
                        },
                        onUpcomingRepeatToggle = { selectedVerse ->
                            val currentMode = upcomingRepeatModes[selectedVerse.id] ?: RepeatMode.Off
                            upcomingRepeatModes = upcomingRepeatModes + (selectedVerse.id to currentMode.next())
                        },
                        onDeleteDueVerse = { selectedVerse ->
                            pendingDelete = PendingDelete(selectedVerse, VerseSection.DueNow)
                        },
                        onEditDueVerse = { selectedVerse ->
                            pendingEdit = PendingEdit(selectedVerse, VerseSection.DueNow)
                        },
                        onDeleteUpcomingVerse = { selectedVerse ->
                            pendingDelete = PendingDelete(selectedVerse, VerseSection.Upcoming)
                        },
                        onEditUpcomingVerse = { selectedVerse ->
                            pendingEdit = PendingEdit(selectedVerse, VerseSection.Upcoming)
                        },
                        onNextVerse = {
                            if (dueVerses.size > 1) {
                                val first = dueVerses.removeAt(0)
                                dueVerses.add(first)
                            } else if (dueVerses.isNotEmpty() && upcomingVerses.isNotEmpty()) {
                                upcomingVerses.add(dueVerses.removeAt(0))
                                dueVerses.add(upcomingVerses.removeAt(0))
                            }
                            hiddenWordCount = 0
                            reviewCompleted = false
                            pendingReviewCompletion = false
                        },
                        hiddenWordCount = hiddenWordCount,
                        isReviewing = isReviewing,
                        onExitReview = {
                            isReviewing = false
                            speaker.stop()
                        },
                        onPreviousVerse = {
                            if (dueVerses.size > 1) {
                                val last = dueVerses.removeAt(dueVerses.lastIndex)
                                dueVerses.add(0, last)
                            } else if (upcomingVerses.isNotEmpty()) {
                                dueVerses.add(0, upcomingVerses.removeAt(upcomingVerses.lastIndex))
                            }
                            hiddenWordCount = 0
                            reviewCompleted = false
                            pendingReviewCompletion = false
                        },
                        reviewContent = {
                            ReviewPracticeCard(
                                verse = verse,
                                verseWords = verseWords,
                                hiddenIndices = reviewHiddenIndices,
                                matchedIndices = matchedIndices,
                                isCompleted = reviewCompleted,
                                showAnswer = showReviewAnswer,
                                repeatMode = repeatMode,
                                isLooping = speaker.isLooping,
                                isListening = voiceRecognizer.isListening,
                                voiceLevel = voiceLevel,
                                onAnswerClick = {
                                    recognizedText = ""
                                    reviewScope.launch {
                                        showReviewAnswer = true
                                        delay(1000)
                                        showReviewAnswer = false
                                    }
                                },
                                onSpeakClick = {
                                    recognizedText = ""
                                    speaker.speak(verse, repeatMode)
                                },
                                onRepeatClick = {
                                    recognizedText = ""
                                    repeatMode = repeatMode.next()
                                },
                                onMicClick = {
                                    if (voiceRecognizer.isListening) {
                                        recognizedText = ""
                                        voiceRecognizer.stopListening()
                                    } else if (
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        voiceRecognizer.startListening()
                                    } else {
                                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            )
                        }
                    )
                }
                BottomTab.Progress -> {
                    ProgressScreen(innerPadding = innerPadding)
                }
                BottomTab.Settings -> {
                    SettingsScreen(
                        innerPadding = innerPadding,
                        selectedBibleVersion = selectedBibleVersion,
                        onVersionSelected = { selectedBibleVersion = it },
                        speechRate = speechRate,
                        onSpeechRateChange = { speechRate = it }
                    )
                }
            }

            if (showCompletionDialog) {
                CompletionDialog(
                    onDismiss = { showCompletionDialog = false }
                )
            }
            pendingDelete?.let { deleteState ->
                DeleteVerseDialog(
                    verseReference = deleteState.verse.reference,
                    onConfirm = {
                        when (deleteState.section) {
                            VerseSection.DueNow -> {
                                val removeIndex = dueVerses.indexOfFirst { it.id == deleteState.verse.id }
                                if (removeIndex >= 0) {
                                    dueVerses.removeAt(removeIndex)
                                }
                            }
                            VerseSection.Upcoming -> {
                                val removeIndex = upcomingVerses.indexOfFirst { it.id == deleteState.verse.id }
                                if (removeIndex >= 0) {
                                    upcomingVerses.removeAt(removeIndex)
                                }
                            }
                        }
                        pendingDelete = null
                    },
                    onDismiss = { pendingDelete = null }
                )
            }
            pendingEdit?.let { editState ->
                EditVerseScreen(
                    verse = editState.verse,
                    initialType = if (editState.section == VerseSection.DueNow) {
                        VerseContentType.DueNow
                    } else {
                        VerseContentType.Upcoming
                    },
                    onCancel = { pendingEdit = null },
                    onSave = { updatedVerse, updatedType ->
                        applyVerseEdit(editState, updatedVerse, updatedType)
                        pendingEdit = null
                    }
                )
            }
            if (showAddVersePage) {
                AddVerseScreen(
                    initialBibleVersion = selectedBibleVersion,
                    onCancel = { showAddVersePage = false },
                    onSave = { newVerse, contentType ->
                        if (contentType == VerseContentType.DueNow) {
                            dueVerses.add(newVerse)
                        } else {
                            upcomingVerses.add(newVerse)
                        }
                        showAddVersePage = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReviewScreen(
    innerPadding: PaddingValues,
    verse: Verse,
    dueCount: Int,
    passCount: Int,
    progressPercent: Int,
    onResetReview: () -> Unit,
    dueVerses: SnapshotStateList<Verse>,
    upcomingVerses: SnapshotStateList<Verse>,
    dueRepeatModes: Map<String, RepeatMode>,
    upcomingRepeatModes: Map<String, RepeatMode>,
    versePassCounts: Map<String, Int>,
    activeDueLooping: Boolean,
    onMoveVerse: (Verse, VerseSection, VerseSection) -> Unit,
    onAddVerse: () -> Unit,
    hiddenWordCount: Int,
    isReviewing: Boolean,
    onPlay: () -> Unit,
    onSpeak: (Verse) -> Unit,
    onUpcomingSpeak: (Verse) -> Unit,
    onRepeatToggle: (Verse) -> Unit,
    onUpcomingRepeatToggle: (Verse) -> Unit,
    onDeleteDueVerse: (Verse) -> Unit,
    onEditDueVerse: (Verse) -> Unit,
    onDeleteUpcomingVerse: (Verse) -> Unit,
    onEditUpcomingVerse: (Verse) -> Unit,
    onNextVerse: () -> Unit,
    onExitReview: () -> Unit,
    onPreviousVerse: () -> Unit,
    reviewContent: @Composable () -> Unit
) {
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var dueBounds by remember { mutableStateOf<Rect?>(null) }
    var upcomingBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TopBar(onAddVerse)
            HeroTitle()
            StatsRow(
                dueCount = dueCount,
                passCount = passCount,
                progressPercent = progressPercent,
                onResetReview = onResetReview
            )
            if (isReviewing) {
                ReviewTransportRow(
                    onPreviousVerse = onPreviousVerse,
                    onExitReview = onExitReview,
                    onNextVerse = onNextVerse
                )
                reviewContent()
            } else {
                PlayPill(onPlay = onPlay)
            }
            Text(
                text = "Due Now",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Column(
                modifier = Modifier.onGloballyPositioned { dueBounds = it.boundsInRoot() },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (dueVerses.isEmpty()) {
                    DropZone(text = "Hold and drag an upcoming verse here")
                } else {
                    dueVerses.forEach { dueVerse ->
                        DraggableVerseCard(
                            verse = dueVerse,
                            section = VerseSection.DueNow,
                            onDragStart = { start -> dragState = DragState(dueVerse, VerseSection.DueNow, start) },
                            onDrag = { delta ->
                                dragState = dragState?.copy(position = dragState!!.position + delta)
                            },
                            onDragEnd = {
                                val target = when {
                                    upcomingBounds?.contains(dragState?.position ?: Offset.Zero) == true -> VerseSection.Upcoming
                                    dueBounds?.contains(dragState?.position ?: Offset.Zero) == true -> VerseSection.DueNow
                                    else -> null
                                }
                                if (target != null) {
                                    onMoveVerse(dueVerse, VerseSection.DueNow, target)
                                }
                                dragState = null
                            }
                        ) {
                            DueVerseCard(
                                verse = dueVerse,
                                passCount = versePassCounts[dueVerse.id] ?: 0,
                                hiddenWordCount = if (dueVerse.id == verse.id) hiddenWordCount else 0,
                                progressPercent = if (dueVerse.id == verse.id) progressPercent else 0,
                                onEdit = { onEditDueVerse(dueVerse) },
                                onSpeak = { onSpeak(dueVerse) },
                                onRepeatToggle = { onRepeatToggle(dueVerse) },
                                onNextVerse = { onDeleteDueVerse(dueVerse) },
                                repeatMode = dueRepeatModes[dueVerse.id] ?: RepeatMode.Off,
                                isLooping = activeDueLooping &&
                                    dueVerse.id == verse.id &&
                                    (dueRepeatModes[dueVerse.id] ?: RepeatMode.Off) == RepeatMode.Infinite
                            )
                        }
                    }
                }
            }
            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Column(
                modifier = Modifier.onGloballyPositioned { upcomingBounds = it.boundsInRoot() },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (upcomingVerses.isEmpty()) {
                    DropZone(text = "Hold and drag a due verse here")
                } else {
                    upcomingVerses.forEach { upcomingVerse ->
                        DraggableVerseCard(
                            verse = upcomingVerse,
                            section = VerseSection.Upcoming,
                            onDragStart = { start -> dragState = DragState(upcomingVerse, VerseSection.Upcoming, start) },
                            onDrag = { delta ->
                                dragState = dragState?.copy(position = dragState!!.position + delta)
                            },
                            onDragEnd = {
                                val target = when {
                                    dueBounds?.contains(dragState?.position ?: Offset.Zero) == true -> VerseSection.DueNow
                                    upcomingBounds?.contains(dragState?.position ?: Offset.Zero) == true -> VerseSection.Upcoming
                                    else -> null
                                }
                                if (target != null) {
                                    onMoveVerse(upcomingVerse, VerseSection.Upcoming, target)
                                }
                                dragState = null
                            }
                        ) {
                            DueVerseCard(
                                verse = upcomingVerse,
                                passCount = versePassCounts[upcomingVerse.id] ?: 0,
                                hiddenWordCount = 0,
                                progressPercent = 0,
                                onEdit = { onEditUpcomingVerse(upcomingVerse) },
                                onSpeak = { onUpcomingSpeak(upcomingVerse) },
                                onRepeatToggle = { onUpcomingRepeatToggle(upcomingVerse) },
                                onNextVerse = { onDeleteUpcomingVerse(upcomingVerse) },
                                repeatMode = upcomingRepeatModes[upcomingVerse.id] ?: RepeatMode.Off,
                                isLooping = false
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        dragState?.let { activeDrag ->
            Box(
                modifier = Modifier
                    .padding(top = 0.dp)
                    .offset {
                        IntOffset(
                            x = activeDrag.position.x.roundToInt() - 170,
                            y = activeDrag.position.y.roundToInt() - 80
                        )
                    }
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(activeDrag.verse.reference, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                        Text(
                            text = activeDrag.verse.text,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewTransportRow(
    onPreviousVerse: () -> Unit,
    onExitReview: () -> Unit,
    onNextVerse: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ReviewTransportButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.NavigateBefore,
            contentDescription = stringResource(R.string.previous_verse),
            onClick = onPreviousVerse,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
        )
        ReviewTransportButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.NotInterested,
            contentDescription = stringResource(R.string.stop_review),
            onClick = onExitReview,
            containerColor = MaterialTheme.colorScheme.primary
        )
        ReviewTransportButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.NavigateNext,
            contentDescription = stringResource(R.string.next_verse),
            onClick = onNextVerse,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
        )
    }
}

@Composable
private fun ReviewTransportButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun ProgressScreen(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(innerPadding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Progress",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Progress screen is next.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Review and Settings are active now. Progress can be added next with charts and verse history.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopBar(onAddVerse: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), CircleShape)
                .clickable(onClick = onAddVerse),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_verse),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun AddVerseScreen(
    initialBibleVersion: String,
    onCancel: () -> Unit,
    onSave: (Verse, VerseContentType) -> Unit
) {
    var selectedBibleVersion by remember { mutableStateOf(initialBibleVersion) }
    var selectedBook by remember { mutableStateOf(bibleBooks.first()) }
    var selectedChapter by remember { mutableIntStateOf(1) }
    var selectedVerseStart by remember { mutableIntStateOf(1) }
    var useVerseEnd by remember { mutableStateOf(false) }
    var selectedVerseEnd by remember { mutableIntStateOf(1) }
    var selectedType by remember { mutableStateOf(VerseContentType.DueNow) }
    var tags by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(VerseDifficulty.Medium) }
    var verseText by remember { mutableStateOf("") }
    var dictatedPrefix by remember { mutableStateOf("") }
    val maxVerse = selectedBook.knownVerseCounts[selectedChapter] ?: 50
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val addVoiceRecognizer = rememberVoiceRecognizer(
        onResult = { spokenText ->
            val updatedText = mergeRecognizedText(dictatedPrefix, spokenText)
            dictatedPrefix = updatedText
            verseText = updatedText
        },
        onPartialResult = { spokenText ->
            verseText = mergeRecognizedText(dictatedPrefix, spokenText)
        },
        onError = {},
        onListeningStateChanged = { _ -> },
        onLevelChanged = {}
    )
    val addVoicePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            addVoiceRecognizer.startListening()
        }
    }

    if (selectedVerseStart > maxVerse) selectedVerseStart = maxVerse
    if (selectedVerseEnd > maxVerse) selectedVerseEnd = maxVerse
    if (selectedVerseEnd < selectedVerseStart) selectedVerseEnd = selectedVerseStart

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundedTextButton("Cancel", onCancel)
                RoundedTextButton(
                    text = "Save",
                    onClick = {
                        val reference = buildString {
                            append(selectedBook.name)
                            append(" ")
                            append(selectedChapter)
                            append(":")
                            append(selectedVerseStart)
                            if (useVerseEnd) {
                                append("-")
                                append(selectedVerseEnd)
                            }
                        }
                        addVoiceRecognizer.stopListening()
                        onSave(
                            Verse(
                                id = "custom-${System.currentTimeMillis()}",
                                reference = reference,
                                translation = selectedBibleVersion,
                                text = verseText,
                                prompt = if (tags.isBlank()) difficulty.label else tags
                            ),
                            selectedType
                        )
                    },
                    enabled = verseText.isNotBlank()
                )
            }
            Text(
                text = "Add Verse",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Reference",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionDropdownField("Bible Version", selectedBibleVersion, bibleVersions) {
                        selectedBibleVersion = it
                    }
                    SelectionDropdownField("Book", selectedBook.name, bibleBooks.map { it.name }) {
                        selectedBook = bibleBooks.first { book -> book.name == it }
                        selectedChapter = 1
                        selectedVerseStart = 1
                        selectedVerseEnd = 1
                    }
                    SelectionDropdownField("Chapter", selectedChapter.toString(), (1..selectedBook.chapterCount).map(Int::toString)) {
                        selectedChapter = it.toInt()
                        selectedVerseStart = 1
                        selectedVerseEnd = 1
                    }
                    SelectionDropdownField("Verse Start", selectedVerseStart.toString(), (1..maxVerse).map(Int::toString)) {
                        selectedVerseStart = it.toInt()
                        if (selectedVerseEnd < selectedVerseStart) selectedVerseEnd = selectedVerseStart
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Use Verse End",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = useVerseEnd,
                            onCheckedChange = { useVerseEnd = it }
                        )
                    }
                    if (useVerseEnd) {
                        SelectionDropdownField("Verse End", selectedVerseEnd.toString(), (selectedVerseStart..maxVerse).map(Int::toString)) {
                            selectedVerseEnd = it.toInt()
                        }
                    }
                }
            }
            Text(
                text = "Content",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionDropdownField("Type", selectedType.label, VerseContentType.entries.map { it.label }) {
                        selectedType = VerseContentType.entries.first { item -> item.label == it }
                    }
                    TextField(
                        value = tags,
                        onValueChange = { tags = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tags (Comma separated)") },
                        colors = darkFieldColors()
                    )
                    SelectionDropdownField("Difficulty", difficulty.label, VerseDifficulty.entries.map { it.label }) {
                        difficulty = VerseDifficulty.entries.first { item -> item.label == it }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Verse Text",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        SmallIconButton(
                            icon = Icons.Filled.KeyboardVoice,
                            label = stringResource(R.string.start_voice_input),
                            tint = if (addVoiceRecognizer.isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            onClick = {
                                if (addVoiceRecognizer.isListening) {
                                    dictatedPrefix = verseText.trim()
                                    addVoiceRecognizer.stopListening()
                                } else if (
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    dictatedPrefix = verseText.trim()
                                    addVoiceRecognizer.startListening()
                                } else {
                                    dictatedPrefix = verseText.trim()
                                    addVoicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }
                    TextField(
                        value = verseText,
                        onValueChange = { verseText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("Verse Text") },
                        colors = darkFieldColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun EditVerseScreen(
    verse: Verse,
    initialType: VerseContentType,
    onCancel: () -> Unit,
    onSave: (Verse, VerseContentType) -> Unit
) {
    var selectedBibleVersion by remember { mutableStateOf(verse.translation) }
    var selectedType by remember { mutableStateOf(initialType) }
    var verseText by remember { mutableStateOf(verse.text) }
    var dictatedPrefix by remember { mutableStateOf(verse.text.trim()) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val editVoiceRecognizer = rememberVoiceRecognizer(
        onResult = { spokenText ->
            val updatedText = mergeRecognizedText(dictatedPrefix, spokenText)
            dictatedPrefix = updatedText
            verseText = updatedText
        },
        onPartialResult = { spokenText ->
            verseText = mergeRecognizedText(dictatedPrefix, spokenText)
        },
        onError = {},
        onListeningStateChanged = { _ -> },
        onLevelChanged = {}
    )
    val editVoicePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            editVoiceRecognizer.startListening()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundedTextButton("Cancel", onCancel)
                RoundedTextButton(
                    text = "Save",
                    onClick = {
                        editVoiceRecognizer.stopListening()
                        onSave(
                            verse.copy(
                                translation = selectedBibleVersion,
                                text = verseText
                            ),
                            selectedType
                        )
                    },
                    enabled = verseText.isNotBlank()
                )
            }
            Text(
                text = "Edit Verse",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Reference",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = verse.reference,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Verse") },
                        colors = darkFieldColors()
                    )
                    SelectionDropdownField("Bible Version", selectedBibleVersion, bibleVersions) {
                        selectedBibleVersion = it
                    }
                }
            }
            Text(
                text = "Verse Text",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionDropdownField("Type", selectedType.label, VerseContentType.entries.map { it.label }) {
                        selectedType = VerseContentType.entries.first { item -> item.label == it }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Verse Text",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        SmallIconButton(
                            icon = Icons.Filled.KeyboardVoice,
                            label = stringResource(R.string.start_voice_input),
                            tint = if (editVoiceRecognizer.isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            onClick = {
                                if (editVoiceRecognizer.isListening) {
                                    dictatedPrefix = verseText.trim()
                                    editVoiceRecognizer.stopListening()
                                } else if (
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    dictatedPrefix = verseText.trim()
                                    editVoiceRecognizer.startListening()
                                } else {
                                    dictatedPrefix = verseText.trim()
                                    editVoicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }
                    TextField(
                        value = verseText,
                        onValueChange = {
                            verseText = it
                            dictatedPrefix = it.trim()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        colors = darkFieldColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundedTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
        )
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            colors = darkFieldColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = MaterialTheme.colorScheme.onBackground) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun darkFieldColors() = ExposedDropdownMenuDefaults.textFieldColors(
    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreen(
    innerPadding: PaddingValues,
    selectedBibleVersion: String,
    onVersionSelected: (String) -> Unit,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(innerPadding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Bible",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedBibleVersion,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(22.dp),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground
                        ),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(20.dp)
                        )
                    ) {
                        bibleVersions.forEach { version ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = version,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                onClick = {
                                    onVersionSelected(version)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = "All verses in the app will use the selected Bible version.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "Speech Speed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SpeechSpeedCard(
            speechRate = speechRate,
            onSpeechRateChange = onSpeechRateChange
        )
    }
}

@Composable
private fun SpeechSpeedCard(
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${speechRate.formatRate()}x",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedAdjustButton(
                    symbol = "-",
                    onClick = {
                        onSpeechRateChange((speechRate - 0.1f).coerceAtLeast(0.1f).snapToTenth())
                    }
                )
                Slider(
                    value = speechRate,
                    onValueChange = { onSpeechRateChange(it.snapToTenth()) },
                    valueRange = 0.1f..2.0f,
                    steps = 18,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                    )
                )
                SpeedAdjustButton(
                    symbol = "+",
                    onClick = {
                        onSpeechRateChange((speechRate + 0.1f).coerceAtMost(2.0f).snapToTenth())
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0.1x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "1.0x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "2.0x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Text(
                text = "1.0x is normal speed. Lower values speak more slowly, and higher values speak faster.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpeedAdjustButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = MaterialTheme.colorScheme.background,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = if (symbol == "-") 2.dp else 0.dp)
        )
    }
}

@Composable
private fun HeroTitle() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Bible Memorize",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
                modifier = Modifier.size(28.dp)
            )
        }
        Box(
            modifier = Modifier
                .width(210.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(100.dp))
        )
    }
}

@Composable
private fun StatsRow(
    dueCount: Int,
    passCount: Int,
    progressPercent: Int,
    onResetReview: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Due",
            value = dueCount.toString()
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Pass",
            value = passCount.toString(),
            badgeIcon = Icons.Filled.RestartAlt,
            onBadgeClick = onResetReview
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Progress",
            value = "$progressPercent%"
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onBadgeClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (badgeIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape)
                            .clickable(enabled = onBadgeClick != null) {
                                onBadgeClick?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun PlayPill(onPlay: () -> Unit) {
    Button(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.play_review),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DraggableVerseCard(
    verse: Verse,
    section: VerseSection,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                cardBounds = coordinates.boundsInRoot()
            }
            .pointerInput(verse.id, section, cardBounds) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val bounds = cardBounds
                        val start = if (bounds != null) {
                            Offset(bounds.left + offset.x, bounds.top + offset.y)
                        } else {
                            offset
                        }
                        onDragStart(start)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(Offset(dragAmount.x, dragAmount.y))
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
    ) {
        content()
    }
}

@Composable
private fun DueVerseCard(
    verse: Verse,
    passCount: Int,
    hiddenWordCount: Int,
    progressPercent: Int,
    onEdit: () -> Unit,
    onSpeak: () -> Unit,
    onRepeatToggle: () -> Unit,
    onNextVerse: () -> Unit,
    repeatMode: RepeatMode,
    isLooping: Boolean
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = verse.reference,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (passCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = passCount.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                ActionRow(
                    onEdit = onEdit,
                    onSpeak = onSpeak,
                    onRepeat = onRepeatToggle,
                    onNextVerse = onNextVerse,
                    repeatMode = repeatMode,
                    isLooping = isLooping
                )
            }
            VersePreview(
                verse = verse,
                hiddenWordCount = hiddenWordCount
            )
            Text(
                text = "Translation: ${verse.translation}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Progress $progressPercent%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ReviewPracticeCard(
    verse: Verse,
    verseWords: List<String>,
    hiddenIndices: Set<Int>,
    matchedIndices: Set<Int>,
    isCompleted: Boolean,
    showAnswer: Boolean,
    repeatMode: RepeatMode,
    isLooping: Boolean,
    isListening: Boolean,
    voiceLevel: Float,
    onAnswerClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onMicClick: () -> Unit
) {
    val fullMatch = isCompleted || (hiddenIndices.isNotEmpty() && hiddenIndices.all { it in matchedIndices })

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = verse.reference,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                ReviewActionRow(
                    repeatMode = repeatMode,
                    isLooping = isLooping,
                    isListening = isListening,
                    onAnswerClick = onAnswerClick,
                    onSpeakClick = onSpeakClick,
                    onRepeatClick = onRepeatClick,
                    onMicClick = onMicClick
                )
            }
            ReviewVerseText(
                verseWords = verseWords,
                hiddenIndices = hiddenIndices,
                matchedIndices = matchedIndices,
                showAnswer = showAnswer,
                fullMatch = fullMatch
            )
            VoiceLevelIndicator(
                level = voiceLevel,
                active = isListening
            )
        }
    }
}

@Composable
private fun CompletionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = {
            Text(
                text = "Congratulations",
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Text(
                text = "You completed the verse review.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DeleteVerseDialog(
    verseReference: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Delete Verse",
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Text(
                text = "Delete $verseReference?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun VoiceLevelIndicator(level: Float, active: Boolean) {
    Row(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(14) { index ->
            val threshold = (index + 1) / 14f
            val lit = active && level >= threshold
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(if (index % 3 == 0) 18.dp else 12.dp)
                    .background(
                        if (lit) Color(0xFFFF4A57) else Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}

@Composable
private fun ReviewActionRow(
    repeatMode: RepeatMode,
    isLooping: Boolean,
    isListening: Boolean,
    onAnswerClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onMicClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ReviewIconButton(
            icon = Icons.Filled.Lightbulb,
            label = stringResource(R.string.answer_peek),
            tint = Color.White,
            backgroundColor = MaterialTheme.colorScheme.primary,
            onClick = onAnswerClick
        )
        ReviewIconButton(
            icon = Icons.Filled.VolumeUp,
            label = stringResource(R.string.speak_verse),
            tint = Color.White,
            backgroundColor = Color.Transparent,
            onClick = onSpeakClick
        )
        ReviewRepeatButton(
            repeatMode = repeatMode,
            isLooping = isLooping,
            onClick = onRepeatClick
        )
        ReviewIconButton(
            icon = Icons.Filled.KeyboardVoice,
            label = stringResource(R.string.voice_check),
            tint = Color.White,
            backgroundColor = if (isListening) Color(0xFFB73A3A) else Color.Transparent,
            borderColor = Color(0xFFB73A3A),
            onClick = onMicClick
        )
    }
}

@Composable
private fun ReviewRepeatButton(
    repeatMode: RepeatMode,
    isLooping: Boolean,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        ReviewIconButton(
            icon = Icons.Filled.Repeat,
            label = stringResource(R.string.repeat_mode),
            tint = if (repeatMode == RepeatMode.Off) Color.White else MaterialTheme.colorScheme.primary,
            backgroundColor = Color.Transparent,
            onClick = onClick
        )
        if (repeatMode != RepeatMode.Off) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (isLooping) MaterialTheme.colorScheme.primary else Color.White,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = repeatMode.badge,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isLooping) Color.Black else MaterialTheme.colorScheme.background
                )
            }
        }
    }
}

@Composable
private fun ReviewIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                if (backgroundColor == Color.Transparent) Color.Transparent else backgroundColor,
                CircleShape
            )
            .border(
                width = if (borderColor == Color.Transparent) 0.dp else 2.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ReviewVerseText(
    verseWords: List<String>,
    hiddenIndices: Set<Int>,
    matchedIndices: Set<Int>,
    showAnswer: Boolean,
    fullMatch: Boolean
) {
    val textColor = if (fullMatch) Color(0xFF47D16A) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f)
    val verseText = buildAnnotatedString {
        verseWords.forEachIndexed { index, word ->
            val visible = showAnswer || fullMatch || index !in hiddenIndices || index in matchedIndices
            withStyle(SpanStyle(color = textColor)) {
                append(if (visible) word else "_".repeat(word.filter { !it.isWhitespace() }.length.coerceAtLeast(2)))
            }
            if (index != verseWords.lastIndex) append(" ")
        }
    }

    Text(
        text = verseText,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun ActionRow(
    onEdit: () -> Unit,
    onSpeak: () -> Unit,
    onRepeat: () -> Unit,
    onNextVerse: () -> Unit,
    repeatMode: RepeatMode,
    isLooping: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        SmallIconButton(
            icon = Icons.Filled.Edit,
            label = "Edit verse",
            tint = MaterialTheme.colorScheme.onBackground,
            onClick = onEdit
        )
        SmallIconButton(
            icon = Icons.Filled.VolumeUp,
            label = stringResource(R.string.speak_verse),
            tint = MaterialTheme.colorScheme.onBackground,
            onClick = onSpeak
        )
        ReviewRepeatButton(
            repeatMode = repeatMode,
            isLooping = isLooping,
            onClick = onRepeat
        )
        SmallIconButton(
            icon = Icons.Filled.DeleteOutline,
            label = stringResource(R.string.next_verse),
            tint = Color(0xFFFF5D5D),
            onClick = onNextVerse
        )
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(30.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun VersePreview(verse: Verse, hiddenWordCount: Int) {
    val previewText = verse.text.split(" ").mapIndexed { index, word ->
        if (index < hiddenWordCount) "_".repeat(word.filter { !it.isWhitespace() }.length.coerceAtLeast(1)) else word
    }.joinToString(" ")

    Text(
        text = previewText,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
        maxLines = 4,
        overflow = TextOverflow.Ellipsis
    )
}

private fun buildReviewHiddenIndices(words: List<String>): Set<Int> =
    words.mapIndexedNotNull { index, word ->
        val normalized = normalizeToken(word)
        if (index % 3 != 1 && normalized.length > 2) index else null
    }.toSet()

private fun matchRecognizedWords(
    verseWords: List<String>,
    hiddenIndices: Set<Int>,
    recognizedText: String
): Set<Int> {
    val spokenTokens = recognizedText.split(" ")
        .map(::normalizeToken)
        .filter { it.isNotBlank() }
        .toSet()

    return hiddenIndices.filter { index ->
        spokenTokens.contains(normalizeToken(verseWords[index]))
    }.toSet()
}

private fun normalizeToken(token: String): String =
    token.lowercase(Locale.US).replace(Regex("[^\\p{L}\\p{N}]"), "")

private const val VERSE_PREFS_NAME = "bible_memorize_verse_store"
private const val DUE_VERSES_KEY = "due_verses"
private const val UPCOMING_VERSES_KEY = "upcoming_verses"
private const val VERSE_PASS_COUNTS_KEY = "verse_pass_counts"

private fun loadPersistedVerseState(context: android.content.Context): Pair<List<Verse>, List<Verse>> {
    val preferences = context.getSharedPreferences(VERSE_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val dueVersesJson = preferences.getString(DUE_VERSES_KEY, null)
    val upcomingVersesJson = preferences.getString(UPCOMING_VERSES_KEY, null)

    if (dueVersesJson.isNullOrBlank() && upcomingVersesJson.isNullOrBlank()) {
        return starterVerses.take(1) to starterVerses.drop(1)
    }

    val dueVerses = dueVersesJson.toVerseList()
    val upcomingVerses = upcomingVersesJson.toVerseList()
    if (dueVerses.isEmpty() && upcomingVerses.isEmpty()) {
        return starterVerses.take(1) to starterVerses.drop(1)
    }

    return dueVerses to upcomingVerses
}

private fun savePersistedVerseState(
    context: android.content.Context,
    dueVerses: List<Verse>,
    upcomingVerses: List<Verse>
) {
    val preferences = context.getSharedPreferences(VERSE_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    preferences.edit()
        .putString(DUE_VERSES_KEY, dueVerses.toJson())
        .putString(UPCOMING_VERSES_KEY, upcomingVerses.toJson())
        .apply()
}

private fun loadPersistedVersePassCounts(context: android.content.Context): Map<String, Int> {
    val preferences = context.getSharedPreferences(VERSE_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val rawValue = preferences.getString(VERSE_PASS_COUNTS_KEY, null).orEmpty()
    if (rawValue.isBlank()) return emptyMap()

    return runCatching {
        val jsonObject = JSONObject(rawValue)
        jsonObject.keys().asSequence().mapNotNull { key ->
            val count = jsonObject.optInt(key, 0)
            if (key.isBlank() || count <= 0) null else key to count
        }.toMap()
    }.getOrDefault(emptyMap())
}

private fun savePersistedVersePassCounts(
    context: android.content.Context,
    passCounts: Map<String, Int>
) {
    val preferences = context.getSharedPreferences(VERSE_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val jsonObject = JSONObject().apply {
        passCounts.forEach { (verseId, count) ->
            if (verseId.isNotBlank() && count > 0) {
                put(verseId, count)
            }
        }
    }
    preferences.edit()
        .putString(VERSE_PASS_COUNTS_KEY, jsonObject.toString())
        .apply()
}

private fun String?.toVerseList(): List<Verse> {
    if (this.isNullOrBlank()) return emptyList()

    return runCatching {
        val jsonArray = JSONArray(this)
        buildList(jsonArray.length()) {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                add(
                    Verse(
                        id = item.optString("id"),
                        reference = item.optString("reference"),
                        translation = item.optString("translation"),
                        text = item.optString("text"),
                        prompt = item.optString("prompt")
                    )
                )
            }
        }.filter { verse ->
            verse.id.isNotBlank() &&
                verse.reference.isNotBlank() &&
                verse.translation.isNotBlank() &&
                verse.text.isNotBlank()
        }
    }.getOrDefault(emptyList())
}

private fun List<Verse>.toJson(): String =
    JSONArray().apply {
        forEach { verse ->
            put(
                JSONObject().apply {
                    put("id", verse.id)
                    put("reference", verse.reference)
                    put("translation", verse.translation)
                    put("text", verse.text)
                    put("prompt", verse.prompt)
                }
            )
        }
    }.toString()

private fun mergeRecognizedText(existing: String, incoming: String): String {
    val current = existing.trim()
    val next = incoming.trim()

    if (next.isBlank()) return current
    if (current.isBlank()) return next
    if (next.equals(current, ignoreCase = true)) return current
    if (next.startsWith(current, ignoreCase = true)) return next
    if (current.startsWith(next, ignoreCase = true)) return current

    return "$current $next"
}

private data class VoiceRecognizerController(
    val startListening: () -> Unit,
    val stopListening: () -> Unit,
    val disableAutoRestart: () -> Unit,
    val isListening: Boolean
)

@Composable
private fun rememberVoiceRecognizer(
    onResult: (String) -> Unit,
    onPartialResult: (String) -> Unit = onResult,
    onError: (String) -> Unit,
    onListeningStateChanged: (Boolean) -> Unit,
    onLevelChanged: (Float) -> Unit
): VoiceRecognizerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnPartialResult by rememberUpdatedState(onPartialResult)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnListeningStateChanged by rememberUpdatedState(onListeningStateChanged)
    val currentOnLevelChanged by rememberUpdatedState(onLevelChanged)
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var shouldKeepListening by remember { mutableStateOf(false) }

    fun buildRecognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(
                RecognizerIntent.EXTRA_SEGMENTED_SESSION,
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS
            )
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 120000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 120000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 120000L)
        }

    DisposableEffect(context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            currentOnError("Speech recognition is not available on this device.")
            onDispose { }
        } else {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    currentOnListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) {
                    currentOnLevelChanged((rmsdB / 10f).coerceIn(0f, 1f))
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    isListening = false
                    currentOnListeningStateChanged(false)
                    currentOnLevelChanged(0f)
                }

                override fun onError(error: Int) {
                    isListening = false
                    currentOnListeningStateChanged(false)
                    currentOnLevelChanged(0f)
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            currentOnError("Didn't catch that. Try speaking again.")
                            shouldKeepListening = false
                        }
                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            currentOnError("")
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            currentOnError("Microphone permission is required.")
                        }
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            currentOnError("Speech recognition network error.")
                            shouldKeepListening = false
                        }
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                            currentOnError("Speech recognition language is not available.")
                            shouldKeepListening = false
                        }
                        else -> {
                            currentOnError("Speech recognition is unavailable right now.")
                            shouldKeepListening = false
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    currentOnListeningStateChanged(false)
                    currentOnLevelChanged(0f)
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    currentOnResult(spokenText)
                    if (spokenText.isNotBlank() && shouldKeepListening) {
                        scope.launch {
                            delay(200)
                            speechRecognizer?.startListening(buildRecognizerIntent())
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val spokenText = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (spokenText.isNotBlank()) {
                        currentOnPartialResult(spokenText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            speechRecognizer = recognizer

            onDispose {
                shouldKeepListening = false
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
                speechRecognizer = null
                isListening = false
            }
        }
    }

    return remember(speechRecognizer, isListening) {
        VoiceRecognizerController(
            startListening = {
                shouldKeepListening = true
                speechRecognizer?.startListening(buildRecognizerIntent())
            },
            stopListening = {
                shouldKeepListening = false
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                isListening = false
                currentOnListeningStateChanged(false)
                currentOnLevelChanged(0f)
            },
            disableAutoRestart = {
                shouldKeepListening = false
            },
            isListening = isListening
        )
    }
}

@Composable
private fun DropZone(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomNavBar(selectedTab: BottomTab, onSelect: (BottomTab) -> Unit) {
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f), RoundedCornerShape(32.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BottomTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Button(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.18f) else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (tab) {
                            BottomTab.Review -> Icons.Filled.MenuBook
                            BottomTab.Progress -> Icons.Filled.AutoGraph
                            BottomTab.Settings -> Icons.Filled.Settings
                        },
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private data class VerseSpeakerController(
    val speak: (Verse, RepeatMode) -> Unit,
    val stop: () -> Unit,
    val isLooping: Boolean
)

@Composable
private fun rememberVerseSpeaker(speechRate: Float): VerseSpeakerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var repeatJob by remember { mutableStateOf<Job?>(null) }
    var isLooping by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = engine?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
                isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        textToSpeech = engine

        onDispose {
            repeatJob?.cancel()
            engine?.stop()
            engine?.shutdown()
            textToSpeech = null
            isReady = false
            isLooping = false
        }
    }

    DisposableEffect(textToSpeech, speechRate, isReady) {
        if (isReady) {
            textToSpeech?.setSpeechRate(speechRate)
        }
        onDispose { }
    }

    return remember(textToSpeech, isReady, speechRate, repeatJob, isLooping) {
        VerseSpeakerController(
            speak = { verse: Verse, repeatMode: RepeatMode ->
                val tts = textToSpeech
                if (isReady && tts != null) {
                    val utterance = "${verse.reference}. ${verse.text}"

                    repeatJob?.cancel()
                    repeatJob = null
                    isLooping = false
                    tts.stop()

                    when (repeatMode) {
                        RepeatMode.Off -> {
                            tts.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, verse.reference)
                        }
                        RepeatMode.Once,
                        RepeatMode.Twice -> {
                            val totalTimes = if (repeatMode == RepeatMode.Once) 2 else 3
                            repeat(totalTimes) { index ->
                                tts.speak(
                                    utterance,
                                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                                    null,
                                    "${verse.reference}-$index"
                                )
                            }
                        }
                        RepeatMode.Infinite -> {
                            isLooping = true
                            repeatJob = scope.launch {
                                while (isActive) {
                                    tts.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, verse.reference)
                                    delay(estimateSpeechDurationMillis(utterance, speechRate))
                                }
                            }
                        }
                    }
                }
            },
            stop = {
                repeatJob?.cancel()
                repeatJob = null
                isLooping = false
                textToSpeech?.stop()
            },
            isLooping = isLooping
        )
    }
}

private fun estimateSpeechDurationMillis(text: String, speechRate: Float): Long {
    val words = text.split(" ").size.coerceAtLeast(1)
    val baseDuration = words * 420L
    return (baseDuration / speechRate.coerceAtLeast(0.1f)).toLong().coerceAtLeast(1200L)
}

private fun Float.snapToTenth(): Float = (this * 10f).roundToInt() / 10f

private fun Float.formatRate(): String = String.format(Locale.US, "%.1f", this)

@Preview(showBackground = true)
@Composable
private fun MemorizePreview() {
    BibleMemorizeTheme {
        MemorizeApp()
    }
}
