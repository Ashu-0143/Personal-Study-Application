package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.db.Flashcard
import com.example.ui.theme.*
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel

@Composable
fun FlashcardWorkspaceScreen(viewModel: StudyViewModel) {
    val flashcards by viewModel.topicFlashcards.collectAsState()
    val isGenerating by viewModel.isGeneratingFlashcards.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()

    val topic = selectedTopic ?: return

    var currentCardIdx by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.navigateTo(StudyScreen.TutorWorkspace) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryTeal)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Adaptive Recall Flashcards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    "Spaced Repetition Practice Center",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryTeal
                )
            }
        }

        if (isGenerating) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = PrimaryTeal)
                    Text("AI is forging customized flashcards...", color = OnSurfaceMuted)
                }
            }
        } else if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(48.dp))
                        Text("No Flashcards Exist for this Topic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                        Text(
                            "Your AI companion can read your learning outlines and generate a set of custom question/answer cards matching SM-2 spacing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.generateAIFlashcards(topic) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("generate_flashcards_button")
                        ) {
                            Text("Generate AI Flashcards")
                        }
                    }
                }
            }
        } else {
            val total = flashcards.size
            val card = flashcards[currentCardIdx]

            // Progress Indicators
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Card ${currentCardIdx + 1} of $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceMuted
                )
                LinearProgressIndicator(
                    progress = { (currentCardIdx + 1).toFloat() / total },
                    color = PrimaryTeal,
                    trackColor = OnSurfaceDarker,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }

            // Interactive Flashcard Box with Flip action
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) TertiaryViolet.copy(alpha = 0.1f) else SurfaceDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { isFlipped = !isFlipped }
            ) {
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
                            imageVector = if (isFlipped) Icons.Default.School else Icons.Default.QuestionMark,
                            contentDescription = null,
                            tint = if (isFlipped) TertiaryViolet else PrimaryTeal,
                            modifier = Modifier.size(32.dp)
                        )

                        Text(
                            text = if (isFlipped) "ANSWER:" else "QUESTION:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFlipped) TertiaryViolet else PrimaryTeal
                        )

                        Text(
                            text = if (isFlipped) card.answer else card.question,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = OnSurfaceText,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Text(
                            text = "Tap Card to Flip",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceMuted
                        )
                    }
                }
            }

            // Repetition feedback buttons
            AnimatedVisibility(
                visible = isFlipped,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Rate your Recall Strength (SM-2):", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple(1, "Forgot", AlertCrimson),
                                Triple(2, "Hard", WarningAmber),
                                Triple(3, "Easy", SuccessGreen)
                            )
                            options.forEach { (score, label, color) ->
                                Button(
                                    onClick = {
                                        viewModel.submitFlashcardReview(card, score)
                                        isFlipped = false
                                        if (currentCardIdx < total - 1) {
                                            currentCardIdx++
                                        } else {
                                            viewModel.navigateTo(StudyScreen.TutorWorkspace)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = color.copy(alpha = 0.2f),
                                        contentColor = color
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("flashcard_score_${score}_button")
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
