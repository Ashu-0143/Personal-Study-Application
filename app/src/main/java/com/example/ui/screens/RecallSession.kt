package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecallSessionScreen(viewModel: StudyViewModel) {
    val questions by viewModel.activeRecallQuestions.collectAsState()
    val currentIndex by viewModel.currentRecallIndex.collectAsState()
    val completedScores by viewModel.recallCompletedScores.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()

    var answerRevealed by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (questions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSlateBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    "Adaptive Recall Clinic",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText,
                    textAlign = TextAlign.Center
                )
                Text(
                    "No active questions loaded. Pick a study workspace inside any semester to generate instant diagnostic checks.",
                    color = OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.navigateTo(StudyScreen.Dashboard) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Return to Dashboard", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val isSessionComplete = completedScores.size == questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Upper Title Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(StudyScreen.Dashboard) },
                modifier = Modifier.testTag("back_from_recall_session")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryTeal)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Active Recall Clinic",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    selectedSubject?.name ?: "All Active Subjects",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryTeal
                )
            }
        }

        if (!isSessionComplete) {
            val progressVal = (currentIndex + 1).toFloat() / questions.size
            val activeQuestion = questions[currentIndex]

            // Progress Indicators
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Retrieval Check ${currentIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(progressVal * 100).toInt()}% Done",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { progressVal },
                    color = PrimaryTeal,
                    trackColor = OnSurfaceDarker,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }

            // Interactive Question Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (answerRevealed) TertiaryViolet.copy(alpha = 0.5f) else OnSurfaceDarker.copy(alpha = 0.4f)
                    )
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category and Topic markers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = PrimaryTeal.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = activeQuestion.categoryType,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = activeQuestion.associatedTopic.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Prompt and Question
                    Text(
                        text = activeQuestion.prompt,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )

                    Text(
                        text = activeQuestion.detailQuestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!answerRevealed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OnSurfaceDarker.copy(alpha = 0.2f))
                                .clickable { answerRevealed = true }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = PrimaryTeal)
                                Text(
                                    "Tap to Reveal Answer Outline",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Attempt content recall in your mind before viewing the key targets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Reveal Core Concept solution
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TertiaryViolet.copy(alpha = 0.08f))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TertiaryViolet, modifier = Modifier.size(16.dp))
                                Text(
                                    "Suggested Core Answer Checklist:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TertiaryViolet
                                )
                            }
                            Text(
                                text = activeQuestion.suggestedAnswer,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceText,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
                            )
                        }
                    }
                }
            }

            // Confidences grading block
            AnimatedVisibility(
                visible = answerRevealed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Grade your retention strength:",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceMuted,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 5 levels rating capsule row
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple("Forgotten", AlertCrimson, "score_forgotten"),
                                Triple("Needs Revision", TertiaryViolet, "score_needs_revision"),
                                Triple("Difficult", WarningAmber, "score_difficult"),
                                Triple("Medium", SecondaryBlue, "score_medium"),
                                Triple("Easy", SuccessGreen, "score_easy")
                            )

                            options.forEach { (label, color, tag) ->
                                Button(
                                    onClick = {
                                        viewModel.submitRecallRating(activeQuestion, label)
                                        answerRevealed = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = color.copy(alpha = 0.15f),
                                        contentColor = color
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag(tag)
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Session Complete Screen! Show Performance Intelligence Summary
            val scoreSummaryScroll = rememberScrollState()

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(scoreSummaryScroll),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        "Recall Diagnostics Finished!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "We have processed your retrieval latency and marked your memory indexes inside the SM-2 scheduling logs. Topics will adjust frequencies adaptively.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = OnSurfaceDarker.copy(alpha = 0.4f), thickness = 1.dp)

                    // Scoring breakdowns
                    Text(
                        "Retention Strength Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    completedScores.forEach { (question, rating) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSlateBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = question.associatedTopic.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceText,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = question.categoryType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceMuted,
                                    textAlign = TextAlign.Start
                                )
                            }

                            val badgeColor = when (rating) {
                                "Forgotten" -> AlertCrimson
                                "Needs Revision" -> TertiaryViolet
                                "Difficult" -> WarningAmber
                                "Medium" -> SecondaryBlue
                                else -> SuccessGreen
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = rating,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Diagnostic complete button
            Button(
                onClick = { viewModel.navigateTo(StudyScreen.Dashboard) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboard_recall_session_completion_btn")
            ) {
                Text("Exit Clinic & Save Logs", fontWeight = FontWeight.Bold)
            }
        }
    }
}
