package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSetupScreen(viewModel: StudyViewModel) {
    var semesterName by remember { mutableStateOf("Fall 2026") }
    
    // Choose standard calendar dates range
    val calendar = Calendar.getInstance()
    var startDate by remember { mutableStateOf(calendar.timeInMillis) }
    
    // End date defaults to 16 weeks study interval
    calendar.add(Calendar.WEEK_OF_YEAR, 16)
    var endDate by remember { mutableStateOf(calendar.timeInMillis) }
    
    // Exam date defaults to the end date of the semester
    var examDate by remember { mutableStateOf(calendar.timeInMillis) }

    var subjectsInput by remember { mutableStateOf("Data Structures, Computer Networks, Linear Algebra, AI Foundations") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.navigateTo(StudyScreen.Dashboard) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryTeal)
            }
            Text(
                "Configure Semester Workspace",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceText
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Semester Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )

                OutlinedTextField(
                    value = semesterName,
                    onValueChange = { semesterName = it },
                    label = { Text("Semester / Cycle Name", color = OnSurfaceMuted) },
                    textStyle = LocalTextStyle.current.copy(color = OnSurfaceText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = OnSurfaceDarker
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("semester_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Date", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { startDate -= 86400000 * 7 }, // -1 week
                                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryTeal)
                            ) {
                                Text("-W")
                            }
                            TextButton(
                                onClick = { startDate += 86400000 * 7 }, // +1 week
                                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryTeal)
                            ) {
                                Text("+W")
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Date", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { endDate -= 86400000 * 7 }, // -1 week
                                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryTeal)
                            ) {
                                Text("-W")
                            }
                            TextButton(
                                onClick = { endDate += 86400000 * 7 }, // +1 week
                                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryTeal)
                            ) {
                                Text("+W")
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Final Expected Exams Date", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(examDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { examDate -= 86400000 * 7 },
                            colors = ButtonDefaults.buttonColors(containerColor = OnSurfaceDarker, contentColor = OnSurfaceText),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("-1 Week")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { examDate += 86400000 * 7 },
                            colors = ButtonDefaults.buttonColors(containerColor = OnSurfaceDarker, contentColor = OnSurfaceText),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("+1 Week")
                        }
                    }
                }
            }
        }

        var studyGoals by remember { mutableStateOf("Secure a GPA of 3.8+. Thoroughly digest core data structure concepts, algorithms, and practical applications. Keep spaced repetition revision on track.") }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Study Goals & Vision",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )

                OutlinedTextField(
                    value = studyGoals,
                    onValueChange = { studyGoals = it },
                    label = { Text("What are your goals for this semester?", color = OnSurfaceMuted) },
                    textStyle = LocalTextStyle.current.copy(color = OnSurfaceText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = OnSurfaceDarker
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("study_goals_input")
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Subject Registry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )

                Text(
                    "Type in your course subjects, separated by commas. Your study companion automatically provisions separate AI workspaces for each subject.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted
                )

                OutlinedTextField(
                    value = subjectsInput,
                    onValueChange = { subjectsInput = it },
                    label = { Text("Subjects list", color = OnSurfaceMuted) },
                    textStyle = LocalTextStyle.current.copy(color = OnSurfaceText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = OnSurfaceDarker
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("subjects_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Workspace default setting includes AI flashcards & adaptive recall tracking.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceMuted
                    )
                }
            }
        }

        Button(
            onClick = {
                val list = subjectsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                viewModel.createSemester(
                    name = semesterName,
                    startDate = startDate,
                    endDate = endDate,
                    examDate = examDate,
                    studyGoals = studyGoals,
                    subjectNames = list
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_semester_button")
        ) {
            Text("Provisions AI Academic Workspace", fontWeight = FontWeight.Bold)
        }
    }
}
