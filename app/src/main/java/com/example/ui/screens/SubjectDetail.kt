package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.Subject
import com.example.data.db.Topic
import com.example.data.db.UploadedFile
import com.example.ui.theme.*
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectDetailScreen(viewModel: StudyViewModel) {
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val isTeachingLoading by viewModel.isTeachingLoading.collectAsState()
    val files by viewModel.uploadedFiles.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    val subject = selectedSubject ?: return

    var activeTabIdx by remember { mutableStateOf(0) } // 0: Dashboard, 1: Outline Tree, 2: Document Vault

    // State for manual topic insertion
    var showManualTopicForm by remember { mutableStateOf(false) }
    var topicTitleInput by remember { mutableStateOf("") }
    var topicImportanceInput by remember { mutableStateOf("Core") }
    var topicDurationInput by remember { mutableStateOf(30f) }

    // State for paste syllabus outline parameters
    var syllabusTextInput by remember { mutableStateOf("") }

    // Filter files specifically registered to this subject workspace
    val subjectFiles = files.filter { it.subjectId == subject.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App workspace bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.navigateTo(StudyScreen.Dashboard) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryTeal)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    text = "Cycle Code: ${subject.subjectCode} • Difficulty Level: ${subject.difficulty}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryTeal
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(subject.color).copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Priority ${subject.priority}",
                    color = Color(subject.color),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Segmented Tab switcher row
        TabRow(
            selectedTabIndex = activeTabIdx,
            containerColor = Color.Transparent,
            contentColor = PrimaryTeal,
            divider = {}
        ) {
            Tab(selected = activeTabIdx == 0, onClick = { activeTabIdx = 0 }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                    Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Module Dashboard")
                }
            }
            Tab(selected = activeTabIdx == 1, onClick = { activeTabIdx = 1 }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Topic Outline Tree")
                }
            }
            Tab(selected = activeTabIdx == 2, onClick = { activeTabIdx = 2 }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("File Repository (${subjectFiles.size})")
                }
            }
        }

        // Expanded Page Layouts
        when (activeTabIdx) {
            0 -> SubjectOverviewLayout(
                subject = subject,
                topics = topics,
                viewModel = viewModel,
                onNavigateToOutline = { activeTabIdx = 1 }
            )
            1 -> SubjectOutlineTreeLayout(
                subject = subject,
                topics = topics,
                viewModel = viewModel,
                showManualTopicForm = showManualTopicForm,
                onToggleManualForm = { showManualTopicForm = !showManualTopicForm },
                topicTitleInput = topicTitleInput,
                onTopicTitleChange = { topicTitleInput = it },
                topicImportanceInput = topicImportanceInput,
                onTopicImportanceChange = { topicImportanceInput = it },
                topicDurationInput = topicDurationInput,
                onTopicDurationChange = { topicDurationInput = it },
                syllabusTextInput = syllabusTextInput,
                onSyllabusTextChange = { syllabusTextInput = it },
                isTeachingLoading = isTeachingLoading
            )
            2 -> SubjectMaterialsVaultLayout(
                subject = subject,
                subjectFiles = subjectFiles,
                uploadProgress = uploadProgress,
                viewModel = viewModel
            )
        }
    }
}

// ==========================================
// TAB 1: SUB-MODULE STUDY DASHBOARD
// ==========================================
@Composable
fun SubjectOverviewLayout(
    subject: Subject,
    topics: List<Topic>,
    viewModel: StudyViewModel,
    onNavigateToOutline: () -> Unit
) {
    val studiedCount = topics.count { it.isCompleted }
    val totalCount = topics.size
    val syllabusPercent = if (totalCount > 0) (studiedCount * 100) / totalCount else subject.completionProgress

    val weakCount = topics.count { it.weakScore > 0.6f }
    val pendingCount = totalCount - studiedCount

    // Dynamic readiness quotients
    val estimationReadiness = if (totalCount > 0) {
        val totalWeights = topics.map { if (it.isCompleted) (1f - it.weakScore) else 0f }.sum()
        ((totalWeights / totalCount) * 100f).toInt()
    } else 0

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Readiness Speedometer metric block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Module Coverage Metrics", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                        Text("Estimated Exam Readiness Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                        Text(
                            "Calibrated from your average knowledge recall feedback, completed structures, and spacing algorithm frequency logs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted
                        )
                    }

                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = maxOf(0.1f, estimationReadiness / 100f),
                            color = Color(subject.color),
                            trackColor = OnSurfaceDarker,
                            strokeWidth = 10.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            "$estimationReadiness%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurfaceText
                        )
                    }
                }
            }
        }

        // Two-column numeric KPI grids
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Syllabus Finished", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$syllabusPercent%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$studiedCount of $totalCount Topics mastered", style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Retention Weak Spots", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$weakCount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (weakCount > 0) AlertCrimson else OnSurfaceText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Topics needing recall soon", style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                    }
                }
            }
        }

        // What to Study recommendation trigger
        if (topics.isNotEmpty()) {
            val primeFocus = topics.filter { !it.isCompleted }.maxByOrNull { it.weakScore } ?: topics.minByOrNull { it.isCompleted }
            primeFocus?.let { focusTopic ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(subject.color).copy(alpha = 0.08f)),
                        border = BorderStroke(1.2.dp, Color(subject.color)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(subject.color))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Module Priority Learning Path recommendation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                            }
                            Text(
                                "Your smart study companion suggests focusing on '${focusTopic.name}' next because it is highly critical to syllabus completeness and currently has high priority index.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceMuted
                            )
                            Button(
                                onClick = {
                                    viewModel.selectTopic(focusTopic)
                                    viewModel.navigateTo(StudyScreen.TutorWorkspace)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(subject.color), contentColor = DarkSlateBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enter Study Session Workspace")
                            }
                        }
                    }
                }
            }
        }

        // Section header for weak topics vs studied topics
        item {
            Text("Module Learning Status Lists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)
        }

        // If no topics outline compiled yet
        if (topics.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(36.dp))
                        Text("Prepare Syllabus roadmap outline", fontWeight = FontWeight.Bold)
                        Text("Navigate to the 'Topic Outline Tree' tab to manually insert course outline chapters or upload syllabus listings to trigger instant study pathways.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, textAlign = TextAlign.Center)
                        Button(
                            onClick = onNavigateToOutline,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg)
                        ) {
                            Text("Setup Outline Roadmaps")
                        }
                    }
                }
            }
        } else {
            // Pending items list block
            val pendingTopics = topics.filter { !it.isCompleted }
            if (pendingTopics.isNotEmpty()) {
                item {
                    Text("Pending Syllabus Chapters", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted, fontWeight = FontWeight.Bold)
                }

                items(pendingTopics.take(3)) { tp ->
                    TopicRowItemCompact(topic = tp) {
                        viewModel.selectTopic(tp)
                        viewModel.navigateTo(StudyScreen.TutorWorkspace)
                    }
                }
            }

            // High priority memorization weaknesses
            val weakTopics = topics.filter { it.weakScore > 0.5f }
            if (weakTopics.isNotEmpty()) {
                item {
                    Text("Highly Vulnerable Topics (High weakness rating)", style = MaterialTheme.typography.labelSmall, color = AlertCrimson, fontWeight = FontWeight.Bold)
                }

                items(weakTopics.take(3)) { tp ->
                    TopicRowItemCompact(topic = tp) {
                        viewModel.selectTopic(tp)
                        viewModel.navigateTo(StudyScreen.TutorWorkspace)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: STRUCTURIZED OUTLINE TREE & CHAPTERS
// ==========================================
@Composable
fun SubjectOutlineTreeLayout(
    subject: Subject,
    topics: List<Topic>,
    viewModel: StudyViewModel,
    showManualTopicForm: Boolean,
    onToggleManualForm: () -> Unit,
    topicTitleInput: String,
    onTopicTitleChange: (String) -> Unit,
    topicImportanceInput: String,
    onTopicImportanceChange: (String) -> Unit,
    topicDurationInput: Float,
    onTopicDurationChange: (Float) -> Unit,
    syllabusTextInput: String,
    onSyllabusTextChange: (String) -> Unit,
    isTeachingLoading: Boolean
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Quick tools buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Syllabus Path Roadmap Outline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleManualForm,
                        colors = ButtonDefaults.buttonColors(containerColor = OnSurfaceDarker, contentColor = PrimaryTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(if (showManualTopicForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showManualTopicForm) "Close Form" else "Add Chapter manually", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                }
            }
        }

        // Manual Course outlines formulation form
        if (showManualTopicForm) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Insert Custom Syllabus Topic node", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryTeal)

                        OutlinedTextField(
                            value = topicTitleInput,
                            onValueChange = onTopicTitleChange,
                            label = { Text("Topic Chapter Title", color = OnSurfaceMuted) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceText),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Importance Weight", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    listOf("Core", "Advanced", "Foundation").forEach { value ->
                                        FilterChip(
                                            selected = topicImportanceInput == value,
                                            onClick = { onTopicImportanceChange(value) },
                                            label = { Text(value) }
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Est Duration", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                                    Text("${topicDurationInput.toInt()} min", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = topicDurationInput,
                                    onValueChange = onTopicDurationChange,
                                    valueRange = 10f..120f,
                                    colors = SliderDefaults.colors(thumbColor = PrimaryTeal, activeTrackColor = PrimaryTeal)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (topicTitleInput.isNotBlank()) {
                                    viewModel.createManualTopic(
                                        subjectId = subject.id,
                                        name = topicTitleInput,
                                        importance = topicImportanceInput,
                                        estimatedTimeMinutes = topicDurationInput.toInt()
                                    )
                                    onTopicTitleChange("")
                                    onToggleManualForm()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enlist New Topic Route")
                        }
                    }
                }
            }
        }

        // Paste Syllabus metadata parser
        if (topics.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = PrimaryTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extract Smart learning schema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            "Paste chapter listings, text book indices, scanned note lines or curriculum guides. Study Companion compiles a hierarchical smart sequence roadmap containing quiz anchors automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted
                        )

                        OutlinedTextField(
                            value = syllabusTextInput,
                            onValueChange = onSyllabusTextChange,
                            placeholder = { Text("E.g., Unit 1: Introduction. Chapter 2: Heap Trees & Complexity metrics...", color = OnSurfaceDarker, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceText),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = OnSurfaceDarker),
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )

                        if (isTeachingLoading) {
                            CircularProgressIndicator(color = PrimaryTeal, modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Button(
                                onClick = {
                                    if (syllabusTextInput.isNotBlank()) {
                                        viewModel.uploadCourseMaterials(viewModel.selectedSubject.value!!, syllabusTextInput)
                                        onSyllabusTextChange("")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Trigger Roadmaps Parsing Engine")
                            }
                        }
                    }
                }
            }
        }

        // Dynamic visual list styled like interconnected node structures (Topic tree)
        if (topics.isEmpty() && !isTeachingLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No structured learning routes set up inside this cycle outline yet.", color = OnSurfaceMuted)
                }
            }
        } else {
            items(topics) { tp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = when {
                        tp.isConfusing -> BorderStroke(1.5.dp, AlertCrimson.copy(alpha = 0.6f))
                        tp.needsRevision -> BorderStroke(1.5.dp, TertiaryViolet.copy(alpha = 0.6f))
                        else -> null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectTopic(tp)
                            viewModel.navigateTo(StudyScreen.TutorWorkspace)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive Node bubble
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(100))
                                .background(if (tp.isCompleted) SuccessGreen.copy(alpha = 0.15f) else OnSurfaceDarker)
                                .clickable {
                                    viewModel.toggleTopicCompleted(tp)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (tp.isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            } else {
                                Text(
                                    tp.orderIndex.toString(),
                                    color = OnSurfaceMuted,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tp.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OnSurfaceDarker.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(tp.importance, color = WarningAmber, style = MaterialTheme.typography.labelSmall)
                                }

                                if (tp.isConfusing) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AlertCrimson.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("⚠️ Confused", color = AlertCrimson, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (tp.needsRevision) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(TertiaryViolet.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("🔄 Needs Revision", color = TertiaryViolet, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    "Est: ${tp.estimatedStudyTimeMinutes} mins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceMuted
                                )

                                Box(
                                    modifier = Modifier.size(4.dp).clip(RoundedCornerShape(100)).background(OnSurfaceMuted)
                                )

                                Text(
                                    "Weakness: ${(tp.weakScore * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tp.weakScore > 0.6f) AlertCrimson else SuccessGreen
                                )
                            }
                        }

                        IconButton(onClick = {
                            viewModel.selectTopic(tp)
                            viewModel.navigateTo(StudyScreen.TutorWorkspace)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Launch coach", tint = PrimaryTeal)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: ASSETS & LOCAL MATERIALS VAULT
// ==========================================
@Composable
fun SubjectMaterialsVaultLayout(
    subject: Subject,
    subjectFiles: List<UploadedFile>,
    uploadProgress: Float?,
    viewModel: StudyViewModel
) {
    var simulatorClickActive by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (simulatorClickActive) PrimaryTeal.copy(alpha = 0.08f) else SurfaceDark
                ),
                border = BorderStroke(1.5.dp, if (simulatorClickActive) PrimaryTeal else OnSurfaceDarker),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { simulatorClickActive = !simulatorClickActive }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (simulatorClickActive) PrimaryTeal else OnSurfaceMuted,
                        modifier = Modifier.size(40.dp)
                    )

                    Text(
                        if (simulatorClickActive) "Select item structure below to parse!" else "Smart Resource Upload Box",
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )

                    Text(
                        "Simulate adding syllabus parameters, chapter notes transcripts, or scanned manuals into this subject workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )

                    if (simulatorClickActive) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf(
                                Triple("Syllabus_Outline.txt", "Syllabus", "12 KB"),
                                Triple("Lecture_Slide_HeapTree.pdf", "Textbook", "2.1 MB"),
                                Triple("Handwritten_Propositions.png", "Notes", "920 KB")
                            ).forEach { (fname, type, size) ->
                                Button(
                                    onClick = {
                                        viewModel.uploadSubjectFile(subject.id, fname, type, size)
                                        simulatorClickActive = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Simulate $fname", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                                }
                            }
                        }
                    }

                    uploadProgress?.let { pr ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Running dynamic file parser...", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal)
                                Text("${(pr * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = pr,
                                color = PrimaryTeal,
                                trackColor = OnSurfaceDarker,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(100))
                            )
                        }
                    }
                }
            }
        }

        if (subjectFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No file assets uploaded inside this workspace yet.", color = OnSurfaceMuted)
                }
            }
        } else {
            items(subjectFiles) { file ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (file.fileType.lowercase()) {
                                        "pdf" -> Icons.Default.PictureAsPdf
                                        "image" -> Icons.Default.Image
                                        else -> Icons.Default.Article
                                    },
                                    contentDescription = null,
                                    tint = Color(subject.color),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(file.name, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                                    Text("Ref Type: ${file.fileType} • Size: ${file.fileSize}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteUploadedFile(file) }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Delete", tint = AlertCrimson.copy(alpha = 0.8f))
                            }
                        }

                        // Extracted chapters metadata preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OnSurfaceDarker.copy(alpha = 0.3f))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Extracted Structural chapters list:", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                Text(
                                    file.extractedChaptersText ?: "Extracting indexing values...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getTopicMasteryLabel(topic: Topic): Color {
    return when {
        !topic.isCompleted -> OnSurfaceMuted
        topic.weakScore > 0.7f -> AlertCrimson
        topic.weakScore in 0.4f..0.7f -> WarningAmber
        topic.revisionFrequency >= 5 && topic.weakScore < 0.2f -> SuccessGreen
        topic.weakScore < 0.3f -> PrimaryTeal
        else -> SecondaryBlue
    }
}

fun getTopicMasteryText(topic: Topic): String {
    return when {
        !topic.isCompleted -> "Not Started"
        topic.weakScore > 0.7f -> "Weak"
        topic.weakScore in 0.4f..0.7f -> "Learning"
        topic.revisionFrequency >= 5 && topic.weakScore < 0.2f -> "Mastered"
        topic.weakScore < 0.3f -> "Strong"
        else -> "Revising"
    }
}

@Composable
fun TopicRowItemCompact(topic: Topic, onClick: () -> Unit) {
    val labelText = getTopicMasteryText(topic)
    val labelColor = getTopicMasteryLabel(topic)

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(100))
                        .background(labelColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        topic.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "⏱️ ${topic.estimatedStudyTimeMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceMuted
                        )
                        if (topic.importance.isNotBlank()) {
                            Text(
                                text = "•  ${topic.importance}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = labelColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
