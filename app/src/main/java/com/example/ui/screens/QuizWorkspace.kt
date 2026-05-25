package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
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
import com.example.data.api.StudyQuizQuestion
import com.example.ui.theme.*
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel

@Composable
fun QuizWorkspaceScreen(viewModel: StudyViewModel) {
    val quiz by viewModel.activeQuiz.collectAsState()
    val isQuizLoading by viewModel.isQuizLoading.collectAsState()

    var currentQuestionIdx by remember { mutableStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }

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
            Text(
                "Active Recall Mini-Quiz",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceText
            )
        }

        if (isQuizLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = PrimaryTeal)
                    Text("AI is forging questions...", color = OnSurfaceMuted)
                }
            }
        } else if (quiz == null || quiz!!.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No quiz session loaded. Back to tutoring to launch concept quiz tests.",
                    color = OnSurfaceMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val totalQuestions = quiz!!.size
            val activeQuestion = quiz!!.getOrElse(currentQuestionIdx) { quiz!!.last() }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Question ${currentQuestionIdx + 1} of $totalQuestions",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceMuted
                    )
                }
                LinearProgressIndicator(
                    progress = { (currentQuestionIdx + 1).toFloat() / totalQuestions },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryTeal,
                    trackColor = OnSurfaceDarker
                )
            }

            // Question Box
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OnSurfaceDarker)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = activeQuestion.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                }
            }

            // Options List
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                activeQuestion.options.forEachIndexed { idx, option ->
                    val isSelected = selectedOptionIdx == idx
                    val isCorrect = idx == activeQuestion.correctIndex

                    val containerColor = when {
                        isAnswerSubmitted && isCorrect -> SuccessGreen.copy(alpha = 0.2f)
                        isAnswerSubmitted && isSelected && !isCorrect -> AlertCrimson.copy(alpha = 0.2f)
                        isSelected -> PrimaryTeal.copy(alpha = 0.15f)
                        else -> SurfaceDark
                    }

                    val borderColor = when {
                        isAnswerSubmitted && isCorrect -> SuccessGreen
                        isAnswerSubmitted && isSelected && !isCorrect -> AlertCrimson
                        isSelected -> PrimaryTeal
                        else -> OnSurfaceDarker
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isAnswerSubmitted) {
                                selectedOptionIdx = idx
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceText,
                            modifier = Modifier.wrapContentHeight()
                        )

                        if (isAnswerSubmitted) {
                            if (isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = SuccessGreen)
                            } else if (isSelected) {
                                Icon(Icons.Default.Block, contentDescription = "Incorrect", tint = AlertCrimson)
                            }
                        }
                    }
                }
            }

            // Professor explanation pane
            AnimatedVisibility(
                visible = isAnswerSubmitted,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Tutor Explanation:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                        Text(
                            text = activeQuestion.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted
                        )
                    }
                }
            }

            // Footer control action Button
            Button(
                onClick = {
                    if (!isAnswerSubmitted) {
                        if (selectedOptionIdx != null) {
                            isAnswerSubmitted = true
                        }
                    } else {
                        if (currentQuestionIdx < totalQuestions - 1) {
                            currentQuestionIdx++
                            selectedOptionIdx = null
                            isAnswerSubmitted = false
                        } else {
                            // Reset and back
                            viewModel.navigateTo(StudyScreen.TutorWorkspace)
                        }
                    }
                },
                enabled = selectedOptionIdx != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quiz_action_button")
            ) {
                Text(
                    text = when {
                        !isAnswerSubmitted -> "Submit Selection"
                        currentQuestionIdx < totalQuestions - 1 -> "Next Concept Question"
                        else -> "Finish & Exit Quiz"
                    },
                    fontWeight = FontWeight.Bold
                )
                if (isAnswerSubmitted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}
