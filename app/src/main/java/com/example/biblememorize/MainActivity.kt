package com.example.biblememorize

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.biblememorize.ui.theme.BibleMemorizeTheme
import java.util.Locale
import kotlin.math.roundToInt

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
    var hiddenWordCount by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableStateOf(BottomTab.Review) }
    var selectedBibleVersion by remember { mutableStateOf("NKJV") }
    var speechRate by remember { mutableStateOf(0.9f) }
    val verse = starterVerses[verseIndex]
    val dueCount = starterVerses.size - verseIndex
    val passCount = verseIndex
    val speakVerse = rememberVerseSpeaker(speechRate)
    val progress by animateFloatAsState(
        targetValue = verse.text.split(" ").let { words ->
            if (words.isEmpty()) 0f else hiddenWordCount.toFloat() / words.size.toFloat()
        },
        label = "memorizeProgress"
    )

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
                    progressPercent = (progress * 100).roundToInt(),
                    onPlay = {
                        val totalWords = verse.text.split(" ").size
                        hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(totalWords)
                        speakVerse(verse)
                    },
                    onSpeak = { speakVerse(verse) },
                    onHideMore = {
                        val totalWords = verse.text.split(" ").size
                        hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(totalWords)
                    },
                    onReveal = { hiddenWordCount = (hiddenWordCount - 2).coerceAtLeast(0) },
                    onNextVerse = {
                        verseIndex = (verseIndex + 1) % starterVerses.size
                        hiddenWordCount = 0
                    },
                    hiddenWordCount = hiddenWordCount
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
    onPlay: () -> Unit,
    onSpeak: () -> Unit,
    onHideMore: () -> Unit,
    onReveal: () -> Unit,
    onNextVerse: () -> Unit
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
        PlayPill(onPlay = onPlay)
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
            onReveal = onReveal,
            onNextVerse = onNextVerse
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
                style = MaterialTheme.typography.headlineMedium,
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
    onReveal: () -> Unit,
    onNextVerse: () -> Unit
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
                    onReveal = onReveal,
                    onNextVerse = onNextVerse
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
private fun ActionRow(
    onHideMore: () -> Unit,
    onSpeak: () -> Unit,
    onReveal: () -> Unit,
    onNextVerse: () -> Unit
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
        SmallIconButton(
            icon = Icons.Filled.SwapHoriz,
            label = stringResource(R.string.reveal_words),
            tint = MaterialTheme.colorScheme.onBackground,
            onClick = onReveal
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

@Composable
private fun rememberVerseSpeaker(speechRate: Float): (Verse) -> Unit {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember { mutableStateOf(false) }

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
            engine?.stop()
            engine?.shutdown()
            textToSpeech = null
            isReady = false
        }
    }

    DisposableEffect(textToSpeech, speechRate, isReady) {
        if (isReady) {
            textToSpeech?.setSpeechRate(speechRate)
        }
        onDispose { }
    }

    return remember(textToSpeech, isReady, speechRate) {
        { verse: Verse ->
            if (isReady) {
                textToSpeech?.speak(
                    "${verse.reference}. ${verse.text}",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    verse.reference
                )
            }
        }
    }
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
