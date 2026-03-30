package com.example.biblememorize

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import com.example.biblememorize.ui.theme.BibleMemorizeTheme
import kotlin.math.roundToInt
import java.util.Locale

data class Verse(
    val reference: String,
    val translation: String,
    val text: String,
    val prompt: String
)

private val starterVerses = listOf(
    Verse(
        reference = "Joshua 1:9",
        translation = "NIV",
        text = "Be strong and courageous. Do not be afraid; do not be discouraged, for the Lord your God will be with you wherever you go.",
        prompt = "What command and promise does God give Joshua?"
    ),
    Verse(
        reference = "Psalm 119:11",
        translation = "NIV",
        text = "I have hidden your word in my heart that I might not sin against you.",
        prompt = "Why hide God's word in your heart?"
    ),
    Verse(
        reference = "Philippians 4:6",
        translation = "NIV",
        text = "Do not be anxious about anything, but in every situation, by prayer and petition, with thanksgiving, present your requests to God.",
        prompt = "How should anxiety be answered?"
    )
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
    var verseIndex by remember { mutableIntStateOf(0) }
    var hiddenWordCount by remember { mutableIntStateOf(0) }
    var showPrompt by remember { mutableStateOf(false) }
    val verse = starterVerses[verseIndex]
    val words = verse.text.split(" ")
    val speakVerse = rememberVerseSpeaker()
    val progress by animateFloatAsState(
        targetValue = if (words.isEmpty()) 0f else hiddenWordCount.toFloat() / words.size.toFloat(),
        label = "memorizeProgress"
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                verse = verse,
                versePosition = verseIndex + 1,
                verseCount = starterVerses.size
            )
            PromptCard(
                prompt = verse.prompt,
                visible = showPrompt,
                onToggle = { showPrompt = !showPrompt }
            )
            ProgressCard(progress = progress, hiddenWordCount = hiddenWordCount, totalWords = words.size)
            VerseCard(
                words = words,
                hiddenWordCount = hiddenWordCount,
                onSpeakVerse = { speakVerse(verse) }
            )
            Controls(
                canHideMore = hiddenWordCount < words.size,
                canReveal = hiddenWordCount > 0,
                onHideMore = { hiddenWordCount = (hiddenWordCount + 2).coerceAtMost(words.size) },
                onReveal = { hiddenWordCount = (hiddenWordCount - 2).coerceAtLeast(0) },
                onReset = { hiddenWordCount = 0 },
                onNextVerse = {
                    verseIndex = (verseIndex + 1) % starterVerses.size
                    hiddenWordCount = 0
                    showPrompt = false
                }
            )
        }
    }
}

@Composable
private fun HeaderCard(verse: Verse, versePosition: Int, verseCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Badge(text = "Verse $versePosition of $verseCount")
            Text(
                text = verse.reference,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = verse.translation,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PromptCard(prompt: String, visible: Boolean, onToggle: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recall prompt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onToggle) {
                    Text(if (visible) "Hide" else "Show")
                }
            }
            AnimatedContent(targetState = visible, label = "promptVisibility") { isVisible ->
                Text(
                    text = if (isVisible) prompt else "Use a short cue before reading the verse.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: Float, hiddenWordCount: Int, totalWords: Int) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Memorization progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
            Text(
                text = "${(progress * 100).roundToInt()}% hidden • $hiddenWordCount of $totalWords words",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VerseCard(words: List<String>, hiddenWordCount: Int, onSpeakVerse: () -> Unit) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Practice",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onSpeakVerse) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.speak_verse)
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                words.forEachIndexed { index, word ->
                    val hidden = index < hiddenWordCount
                    WordChip(word = word, hidden = hidden)
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

@Composable
private fun WordChip(word: String, hidden: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (hidden) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (hidden) "_".repeat(word.trim { it.isWhitespace() }.length.coerceAtLeast(1)) else word,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.alpha(if (hidden) 0.9f else 1f)
        )
    }
}

@Composable
private fun Controls(
    canHideMore: Boolean,
    canReveal: Boolean,
    onHideMore: () -> Unit,
    onReveal: () -> Unit,
    onReset: () -> Unit,
    onNextVerse: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onHideMore,
            enabled = canHideMore,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Hide 2 more words")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onReveal,
                enabled = canReveal,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Reveal")
            }
            Button(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Reset")
            }
        }
        Button(
            onClick = onNextVerse,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("Next verse")
        }
    }
}

@Composable
private fun Badge(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .width(10.dp)
                .height(10.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorizePreview() {
    BibleMemorizeTheme {
        MemorizeApp()
    }
}
