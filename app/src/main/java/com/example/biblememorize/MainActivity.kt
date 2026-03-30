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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    val verse = starterVerses[verseIndex]
    val words = verse.text.split(" ")
    val dueCount = starterVerses.size - verseIndex
    val passCount = verseIndex
    val speakVerse = rememberVerseSpeaker()
    val progress by animateFloatAsState(
        targetValue = if (words.isEmpty()) 0f else hiddenWordCount.toFloat() / words.size.toFloat(),
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
                progressPercent = (progress * 100).roundToInt()
            )
            PlayPill(
                onPlay = {
                    hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(words.size)
                    speakVerse(verse)
                }
            )
            Text(
                text = "Due Now",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            DueVerseCard(
                verse = verse,
                hiddenWordCount = hiddenWordCount,
                progressPercent = (progress * 100).roundToInt(),
                onSpeak = { speakVerse(verse) },
                onHideMore = { hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(words.size) },
                onReveal = { hiddenWordCount = (hiddenWordCount - 2).coerceAtLeast(0) },
                onNextVerse = {
                    verseIndex = (verseIndex + 1) % starterVerses.size
                    hiddenWordCount = 0
                }
            )
            DropZone(text = "Hold and drag an upcoming verse here")
            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            DropZone(text = "Hold and drag a due verse here")
            PracticeSection(
                words = words,
                hiddenWordCount = hiddenWordCount,
                prompt = verse.prompt,
                onSpeakVerse = { speakVerse(verse) },
                onReset = { hiddenWordCount = 0 }
            )
            Spacer(modifier = Modifier.height(16.dp))
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
private fun PracticeSection(
    words: List<String>,
    hiddenWordCount: Int,
    prompt: String,
    onSpeakVerse: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
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
                    text = "Practice",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    SmallIconButton(
                        icon = Icons.Filled.VolumeUp,
                        label = stringResource(R.string.speak_verse),
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onSpeakVerse
                    )
                    SmallIconButton(
                        icon = Icons.Filled.AutoGraph,
                        label = stringResource(R.string.reset_progress),
                        tint = MaterialTheme.colorScheme.onBackground,
                        onClick = onReset
                    )
                }
            }
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            WordGrid(
                words = words,
                hiddenWordCount = hiddenWordCount
            )
        }
    }
}

@Composable
private fun WordGrid(words: List<String>, hiddenWordCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val rows = words.chunked(4)
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { wordIndex, word ->
                    val absoluteIndex = rowIndex * 4 + wordIndex
                    WordChip(word = word, hidden = absoluteIndex < hiddenWordCount)
                }
            }
        }
    }
}

@Composable
private fun WordChip(word: String, hidden: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (hidden) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (hidden) "_".repeat(word.filter { !it.isWhitespace() }.length.coerceAtLeast(1)) else word,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.alpha(if (hidden) 0.9f else 1f)
        )
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
private fun rememberVerseSpeaker(): (Verse) -> Unit {
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

    return remember(textToSpeech, isReady) {
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

@Preview(showBackground = true)
@Composable
private fun MemorizePreview() {
    BibleMemorizeTheme {
        MemorizeApp()
    }
}
