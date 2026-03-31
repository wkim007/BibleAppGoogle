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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.biblememorize.ui.theme.BibleMemorizeTheme
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Verse(
    val reference: String,
    val translation: String,
    val text: String,
    val prompt: String
)

private val starterVerses = listOf(
    Verse(
        reference = "Joshua 1:9",
        translation = "NKJV",
        text = "Have I not commanded you? Be strong and of good courage; do not be afraid, nor be dismayed, for the Lord your God is with you wherever you go.",
        prompt = "Courage comes with God's presence."
    ),
    Verse(
        reference = "Psalm 119:11",
        translation = "NIV",
        text = "I have hidden your word in my heart that I might not sin against you.",
        prompt = "Hide Scripture in your heart."
    ),
    Verse(
        reference = "Philippians 4:6",
        translation = "NIV",
        text = "Do not be anxious about anything, but in every situation, by prayer and petition, with thanksgiving, present your requests to God.",
        prompt = "Trade anxiety for prayer."
    )
)

private val bibleVersions = listOf("KJV", "NKJV", "개역한글", "中文", "Español", "日本語", "Deutsch")

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
    var verseIndex by remember { mutableIntStateOf(0) }
    var passCount by remember { mutableIntStateOf(0) }
    var hiddenWordCount by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableStateOf(BottomTab.Review) }
    var selectedBibleVersion by remember { mutableStateOf("NKJV") }
    var speechRate by remember { mutableStateOf(0.9f) }
    var isReviewing by remember { mutableStateOf(false) }
    var showReviewAnswer by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(RepeatMode.Off) }
    var dueRepeatMode by remember { mutableStateOf(RepeatMode.Off) }
    var recognizedText by remember { mutableStateOf("") }
    var matchedIndices by remember { mutableStateOf(setOf<Int>()) }
    var reviewCompleted by remember { mutableStateOf(false) }
    var voiceLevel by remember { mutableStateOf(0f) }
    val verse = starterVerses[verseIndex]
    val verseWords = remember(verse.reference) { verse.text.split(" ") }
    val reviewHiddenIndices = remember(verse.reference) { buildReviewHiddenIndices(verseWords) }
    val dueCount = 1
    val reviewScope = rememberCoroutineScope()
    val speaker = rememberVerseSpeaker(speechRate)
    val voiceRecognizer = rememberVoiceRecognizer(
        onResult = { spokenText ->
            recognizedText = spokenText
            matchedIndices = matchRecognizedWords(
                verseWords = verseWords,
                hiddenIndices = reviewHiddenIndices,
                recognizedText = spokenText
            )
        },
        onError = {
            recognizedText = ""
            voiceLevel = 0f
        },
        onListeningStateChanged = { listening ->
            if (listening) {
                recognizedText = "Listening..."
            } else {
                recognizedText = ""
            }
        },
        onLevelChanged = { voiceLevel = it }
    )
    val progress by animateFloatAsState(
        targetValue = if (verseWords.isEmpty()) 0f else hiddenWordCount.toFloat() / verseWords.size.toFloat(),
        label = "memorizeProgress"
    )
    val reviewFullyMatched = reviewHiddenIndices.isNotEmpty() && reviewHiddenIndices.all { it in matchedIndices }
    val displayProgressPercent = if (reviewCompleted || reviewFullyMatched) 100 else (progress * 100).roundToInt()
    val context = LocalContext.current
    val microphonePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecognizer.startListening()
        } else {
            recognizedText = "Microphone permission is required."
        }
    }

    LaunchedEffect(verseIndex, isReviewing) {
        showReviewAnswer = false
        recognizedText = ""
        matchedIndices = emptySet()
        reviewCompleted = false
        voiceLevel = 0f
        if (!isReviewing) {
            repeatMode = RepeatMode.Off
            speaker.stop()
            voiceRecognizer.stopListening()
        }
    }

    LaunchedEffect(reviewFullyMatched) {
        if (reviewFullyMatched && !reviewCompleted) {
            reviewCompleted = true
            passCount += 1
            recognizedText = ""
            voiceRecognizer.stopListening()
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
        when (selectedTab) {
            BottomTab.Review -> {
                ReviewScreen(
                    innerPadding = innerPadding,
                    verse = verse,
                    dueCount = dueCount,
                    passCount = passCount,
                    progressPercent = displayProgressPercent,
                    onPlay = {
                        val totalWords = verseWords.size
                        hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(totalWords)
                        isReviewing = true
                    },
                    onSpeak = { speaker.speak(verse, dueRepeatMode) },
                    onHideMore = {
                        val totalWords = verseWords.size
                        hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(totalWords)
                    },
                    onRepeatToggle = { dueRepeatMode = dueRepeatMode.next() },
                    onNextVerse = {
                        verseIndex = (verseIndex + 1) % starterVerses.size
                        hiddenWordCount = 0
                        dueRepeatMode = RepeatMode.Off
                        reviewCompleted = false
                    },
                    dueRepeatMode = dueRepeatMode,
                    dueIsLooping = speaker.isLooping && dueRepeatMode == RepeatMode.Infinite,
                    hiddenWordCount = hiddenWordCount,
                    isReviewing = isReviewing,
                    onExitReview = {
                        isReviewing = false
                        speaker.stop()
                    },
                    onPreviousVerse = {
                        verseIndex = if (verseIndex == 0) starterVerses.lastIndex else verseIndex - 1
                        hiddenWordCount = 0
                        reviewCompleted = false
                    },
                    reviewContent = {
                        ReviewPracticeCard(
                            verse = verse,
                            verseWords = verseWords,
                            hiddenIndices = reviewHiddenIndices,
                            matchedIndices = matchedIndices,
                            showAnswer = showReviewAnswer,
                            recognizedText = recognizedText,
                            repeatMode = repeatMode,
                            isLooping = speaker.isLooping,
                            isListening = voiceRecognizer.isListening,
                            voiceLevel = voiceLevel,
                            onAnswerClick = {
                                reviewScope.launch {
                                    showReviewAnswer = true
                                    delay(1000)
                                    showReviewAnswer = false
                                }
                            },
                            onSpeakClick = { speaker.speak(verse, repeatMode) },
                            onRepeatClick = { repeatMode = repeatMode.next() },
                            onMicClick = {
                                if (voiceRecognizer.isListening) {
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
    }
}

@Composable
private fun ReviewScreen(
    innerPadding: PaddingValues,
    verse: Verse,
    dueCount: Int,
    passCount: Int,
    progressPercent: Int,
    hiddenWordCount: Int,
    dueRepeatMode: RepeatMode,
    dueIsLooping: Boolean,
    isReviewing: Boolean,
    onPlay: () -> Unit,
    onSpeak: () -> Unit,
    onHideMore: () -> Unit,
    onRepeatToggle: () -> Unit,
    onNextVerse: () -> Unit,
    onExitReview: () -> Unit,
    onPreviousVerse: () -> Unit,
    reviewContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(innerPadding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TopBar()
        HeroTitle()
        StatsRow(
            dueCount = dueCount,
            passCount = passCount,
            progressPercent = progressPercent
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
        DueVerseCard(
            verse = verse,
            hiddenWordCount = hiddenWordCount,
            progressPercent = progressPercent,
            onSpeak = onSpeak,
            onHideMore = onHideMore,
            onRepeatToggle = onRepeatToggle,
            onNextVerse = onNextVerse,
            repeatMode = dueRepeatMode,
            isLooping = dueIsLooping
        )
        DropZone(text = "Hold and drag an upcoming verse here")
        Text(
            text = "Upcoming",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        DropZone(text = "Hold and drag a due verse here")
        Spacer(modifier = Modifier.height(16.dp))
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
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), CircleShape),
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
private fun StatsRow(dueCount: Int, passCount: Int, progressPercent: Int) {
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
            badgeIcon = Icons.Filled.PlayArrow
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
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
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
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
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
private fun DueVerseCard(
    verse: Verse,
    hiddenWordCount: Int,
    progressPercent: Int,
    onSpeak: () -> Unit,
    onHideMore: () -> Unit,
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "2",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                ActionRow(
                    onHideMore = onHideMore,
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
    showAnswer: Boolean,
    recognizedText: String,
    repeatMode: RepeatMode,
    isLooping: Boolean,
    isListening: Boolean,
    voiceLevel: Float,
    onAnswerClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onMicClick: () -> Unit
) {
    val fullMatch = hiddenIndices.isNotEmpty() && hiddenIndices.all { it in matchedIndices }

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
            if (recognizedText.isNotBlank() || isListening) {
                Text(
                    text = if (recognizedText.isNotBlank()) recognizedText else "Listening...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
    onHideMore: () -> Unit,
    onSpeak: () -> Unit,
    onRepeat: () -> Unit,
    onNextVerse: () -> Unit,
    repeatMode: RepeatMode,
    isLooping: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        SmallIconButton(
            icon = Icons.Filled.Edit,
            label = stringResource(R.string.hide_more),
            tint = MaterialTheme.colorScheme.onBackground,
            onClick = onHideMore
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

private data class VoiceRecognizerController(
    val startListening: () -> Unit,
    val stopListening: () -> Unit,
    val isListening: Boolean
)

@Composable
private fun rememberVoiceRecognizer(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onListeningStateChanged: (Boolean) -> Unit,
    onLevelChanged: (Float) -> Unit
): VoiceRecognizerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
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
                        currentOnResult(spokenText)
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
