package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel

@Composable
fun PreExamPrepScreen(viewModel: StudyViewModel) {
    val condensedContent by viewModel.condensedPrepContent.collectAsState()
    val isPrepLoading by viewModel.isPrepLoading.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()

    val subject = selectedSubject ?: return
    val scrollState = rememberScrollState()

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
            IconButton(onClick = { viewModel.navigateTo(StudyScreen.SubjectDetail) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryTeal)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Condensed Prep Materials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    "Subject: ${subject.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryTeal
                )
            }
        }

        // Student warning advice
        Card(
            colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningAmber)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Rest Tip", tint = WarningAmber)
                Column {
                    Text(
                        "Study Echo's Rest Advice:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                    Text(
                        "Last-minute guides are super effective, but a fully rested brain retains 40% more detail than a sleepless one. Balance revision with proper sleep!",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceText
                    )
                }
            }
        }

        // Main sheet content card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(20.dp)
        ) {
            if (isPrepLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PrimaryTeal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI is synthesizing condensed reviews...", color = OnSurfaceMuted)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = condensedContent ?: "Preparing revision guide...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceText,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.navigateTo(StudyScreen.SubjectDetail) },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("exit_exam_prep_button")
        ) {
            Text("Complete Review & Exit", fontWeight = FontWeight.Bold)
        }
    }
}
