package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.db.Topic
import com.example.ui.theme.*
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel

enum class MasteryLevel(val label: String, val color: Color) {
    NEW("New", Color(0xFF64748B)),
    LEARNING("Learning", Color(0xFF3B82F6)),
    PRACTICING("Practicing", Color(0xFFEAB308)),
    WEAK("Weak Topic", Color(0xFFEF4444)),
    REVISED("Revised", Color(0xFF8B5CF6)),
    MASTERED("Mastered", Color(0xFF10B981))
}

fun getTopicMastery(topic: Topic): MasteryLevel {
    return when {
        topic.isCompleted && topic.weakScore <= 0.25f -> MasteryLevel.MASTERED
        topic.needsRevision -> MasteryLevel.REVISED
        topic.isConfusing || topic.weakScore > 0.7f -> MasteryLevel.WEAK
        topic.studyCount > 2 -> MasteryLevel.PRACTICING
        topic.studyCount > 0 -> MasteryLevel.LEARNING
        else -> MasteryLevel.NEW
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TutorWorkspaceScreen(viewModel: StudyViewModel) {
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val teachingContent by viewModel.teachingContent.collectAsState()
    val isTeachingLoading by viewModel.isTeachingLoading.collectAsState()
    val selectedExplanationMode by viewModel.selectedExplanationMode.collectAsState()
    val flashcards by viewModel.topicFlashcards.collectAsState()
    val explanationHistory by viewModel.explanationHistory.collectAsState()
    val notesMap by viewModel.topicNotes.collectAsState()
    val followUps by viewModel.followUps.collectAsState()
    val isFollowUpLoading by viewModel.isFollowUpLoading.collectAsState()

    val topic = selectedTopic ?: return
    val subject = selectedSubject ?: return

    val scrollState = rememberScrollState()
    var topicSelectorExpanded by remember { mutableStateOf(false) }
    var sidebarExpanded by remember { mutableStateOf(true) }
    var activeRightTab by remember { mutableStateOf(0) } // 0: Notes, 1: AI Q&A, 2: History

    // Retrieve active student quick notes
    val activeNotes = notesMap[topic.id] ?: ""
    var noteInput by remember(topic.id) { mutableStateOf(activeNotes) }
    var followUpInput by remember { mutableStateOf("") }

    // Dynamic chapters layout grouping
    val chapters = remember(topics) {
        val groups = sortedMapOf<String, MutableList<Topic>>()
        topics.forEachIndexed { index, tp ->
            val match = Regex("^(Chapter|Unit|Module)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(tp.name)
            val groupKey = if (match != null) {
                match.value
            } else {
                // Squeeze into units of 3 to outline beautifully
                val unitNum = (index / 3) + 1
                "Chapter $unitNum: Study Outline"
            }
            if (groups[groupKey] == null) {
                groups[groupKey] = mutableListOf()
            }
            groups[groupKey]?.add(tp)
        }
        groups
    }

    // Keep track of expanded state for each virtual chapter
    val expandedChapters = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(chapters) {
        chapters.keys.forEach { key ->
            if (expandedChapters[key] == null) {
                expandedChapters[key] = true
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        // ===========================================================
        // COLUMN 1: COLLAPSIBLE STRUCTURED TOPIC NAVIGATION PANEL
        // ===========================================================
        AnimatedVisibility(
            visible = sidebarExpanded,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(SurfaceDark)
                    .border(BorderStroke(1.dp, OnSurfaceDarker.copy(alpha = 0.5f)))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Syllabus Outline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                    IconButton(onClick = { sidebarExpanded = false }) {
                        Icon(Icons.Default.MenuOpen, contentDescription = "Collapse", tint = PrimaryTeal)
                    }
                }

                Divider(color = OnSurfaceDarker.copy(alpha = 0.3f))

                // Scrollable chapters outline tree
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chapters.forEach { (chapterName, chapterTopics) ->
                        item {
                            val isExp = expandedChapters[chapterName] ?: true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { expandedChapters[chapterName] = !isExp }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isExp) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = chapterName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isExp) {
                                Column(
                                    modifier = Modifier.padding(start = 14.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    chapterTopics.forEach { tp ->
                                        val isSelected = tp.id == topic.id
                                        val mastery = getTopicMastery(tp)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) OnSurfaceDarker.copy(alpha = 0.25f) else Color.Transparent)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) PrimaryTeal.copy(alpha = 0.5f) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { viewModel.selectTopic(tp) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (tp.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (tp.isCompleted) SuccessGreen else OnSurfaceMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tp.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) OnSurfaceText else OnSurfaceMuted,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Mastery levels showing New, Learning, Practicing, Weak, Revised, and Mastered
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(mastery.color.copy(alpha = 0.15f))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = mastery.label,
                                                            color = mastery.color,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    if (tp.isConfusing) {
                                                        Text("⚠️ Confused", color = AlertCrimson, style = MaterialTheme.typography.labelSmall, fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f)
                                                    }
                                                    if (tp.needsRevision) {
                                                        Text("🔄 Revise", color = TertiaryViolet, style = MaterialTheme.typography.labelSmall, fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${tp.estimatedStudyTimeMinutes}m",
                                                color = OnSurfaceDarker,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mini rail indicator when sidebar is collapsed
        if (!sidebarExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(SurfaceDark)
                    .border(BorderStroke(1.dp, OnSurfaceDarker.copy(alpha = 0.5f)))
                    .clickable { sidebarExpanded = true },
                contentAlignment = Alignment.TopCenter
            ) {
                IconButton(onClick = { sidebarExpanded = true }, modifier = Modifier.padding(top = 16.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Expand Outline", tint = PrimaryTeal)
                }
            }
        }

        // ===========================================================
        // COLUMN 2: CENTER AI TEACHING WORKSPACE
        // ===========================================================
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.navigateTo(StudyScreen.SubjectDetail) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryTeal)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { topicSelectorExpanded = !topicSelectorExpanded }
                    ) {
                        Text(
                            text = topic.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceText
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryTeal)
                    }
                    Text(
                        text = "Intellectual Study Desk • ${subject.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryTeal
                    )
                }

                DropdownMenu(
                    expanded = topicSelectorExpanded,
                    onDismissRequest = { topicSelectorExpanded = false },
                    modifier = Modifier.background(SurfaceDark).width(280.dp)
                ) {
                    topics.forEach { tp ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    tp.name,
                                    color = if (tp.id == topic.id) PrimaryTeal else OnSurfaceText,
                                    fontWeight = if (tp.id == topic.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.selectTopic(tp)
                                topicSelectorExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (tp.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (tp.isCompleted) SuccessGreen else OnSurfaceMuted
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(subject.color).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(subject.subjectCode, color = Color(subject.color), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Quick Interactive Status Highlights (Confusing / Need Revision Card controls)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, OnSurfaceDarker.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Completion Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.toggleTopicCompleted(topic) }
                            .padding(end = 8.dp)
                    ) {
                        Checkbox(
                            checked = topic.isCompleted,
                            onCheckedChange = { viewModel.toggleTopicCompleted(topic) },
                            colors = CheckboxDefaults.colors(checkedColor = SuccessGreen, uncheckedColor = OnSurfaceDarker)
                        )
                        Text(
                            text = if (topic.isCompleted) "Done Studying" else "Mark Done",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (topic.isCompleted) SuccessGreen else OnSurfaceText
                        )
                    }

                    // Toggles for Weak indicators: Mark Confusing and Need Revision
                    Button(
                        onClick = { viewModel.toggleTopicConfusing(topic) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (topic.isConfusing) AlertCrimson.copy(alpha = 0.25f) else OnSurfaceDarker.copy(alpha = 0.2f),
                            contentColor = if (topic.isConfusing) AlertCrimson else OnSurfaceText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (topic.isConfusing) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confused", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { viewModel.toggleTopicNeedsRevision(topic) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (topic.needsRevision) TertiaryViolet.copy(alpha = 0.25f) else OnSurfaceDarker.copy(alpha = 0.2f),
                            contentColor = if (topic.needsRevision) TertiaryViolet else OnSurfaceText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.OfflineBolt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Needs Revision", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Stats indicators
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Study Blocks", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                        Text("${topic.studyCount} sessions completed", fontWeight = FontWeight.Bold, color = OnSurfaceText, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Tabbed Explanation Styles Row
            ScrollableTabRow(
                selectedTabIndex = getTabIdx(selectedExplanationMode),
                containerColor = Color.Transparent,
                contentColor = PrimaryTeal,
                edgePadding = 0.dp,
                divider = {}
            ) {
                val modes = listOf(
                    "Simple Explanation",
                    "Beginner-Friendly Teaching",
                    "Detailed Concept Teaching",
                    "Exam-Oriented Teaching",
                    "Quick Revision Mode",
                    "Real-World Analogy Mode",
                    "Step-by-Step Breakdown Mode",
                    "Concept Reinforcement Mode",
                    "Last-Minute Exam Preparation Mode",
                    "Active Recall Teaching Mode"
                )
                modes.forEach { m ->
                    Tab(
                        selected = selectedExplanationMode == m,
                        onClick = { viewModel.selectExplanationMode(m) },
                        text = { Text(m, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Central Workspace Reading Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(BorderStroke(1.dp, OnSurfaceDarker.copy(alpha = 0.5f)))
                    .padding(20.dp)
            ) {
                if (isTeachingLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryTeal)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Study Echo synthesizing explanation...",
                            color = OnSurfaceMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (teachingContent != null) {
                            FormattedTeachingContent(text = teachingContent!!)
                        } else {
                            Text(
                                text = "Select any of the study modes above to initiate tutoring. The AI coach will compile personalized learning materials designed specifically for your optimal recall.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Active Recall Interactive Clinic Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.loadInteractiveQuiz(topic, subject.name)
                                    viewModel.navigateTo(StudyScreen.QuizWorkspace)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue, contentColor = OnSurfaceText),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_quiz_button")
                            ) {
                                Icon(Icons.Default.HelpCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Launch Interactive Quiz")
                            }

                            Button(
                                onClick = { viewModel.navigateTo(StudyScreen.FlashcardWorkspace) },
                                colors = ButtonDefaults.buttonColors(containerColor = TertiaryViolet, contentColor = OnSurfaceText),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("flashcards_button")
                            ) {
                                Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Recall Flashcards (${flashcards.size})")
                            }
                        }
                    }
                }
            }

            // ===========================================================
            // SUB-SECTION: SM-2 ACTIVE RECALL CALIBRATION BAR
            // ===========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rate Your Active Retention",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ratings = listOf(
                            Pair(1, "Forgot / Struggled"),
                            Pair(2, "Ok / Understood"),
                            Pair(3, "Fully Mastered")
                        )
                        ratings.forEach { (rating, label) ->
                            Button(
                                onClick = {
                                    viewModel.toggleTopicCompleted(topic)
                                    viewModel.updateSpacedRepetition(topic, rating)
                                    viewModel.navigateTo(StudyScreen.SubjectDetail)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (rating) {
                                        1 -> AlertCrimson.copy(alpha = 0.15f)
                                        2 -> WarningAmber.copy(alpha = 0.15f)
                                        else -> SuccessGreen.copy(alpha = 0.15f)
                                    },
                                    contentColor = when (rating) {
                                        1 -> AlertCrimson
                                        2 -> WarningAmber
                                        else -> SuccessGreen
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_study_${rating}_button")
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // ===========================================================
        // COLUMN 3: RIGHT SCHOLAR PRODUCTIVITY PANEL (TABBED)
        // ===========================================================
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(SurfaceDark)
                .border(BorderStroke(1.dp, OnSurfaceDarker.copy(alpha = 0.5f)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tab Selectors
            TabRow(
                selectedTabIndex = activeRightTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryTeal,
                divider = {}
            ) {
                Tab(
                    selected = activeRightTab == 0,
                    onClick = { activeRightTab = 0 },
                    text = { Text("Study Notes", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = activeRightTab == 1,
                    onClick = { activeRightTab = 1 },
                    text = { Text("AI Q&A", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = activeRightTab == 2,
                    onClick = { activeRightTab = 2 },
                    text = { Text("History", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }

            Divider(color = OnSurfaceDarker.copy(alpha = 0.3f))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeRightTab) {
                    0 -> {
                        // TAB 1: PERSISTENT HIGH-FIDELITY NOTES
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Topic-Bound Notes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                            }
                            Text(
                                text = "Self-explanation boosts active learning. Key annotations are bound directly to this exact topic.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceMuted
                            )

                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = {
                                    noteInput = it
                                    viewModel.saveTopicNotes(topic.id, it)
                                },
                                placeholder = {
                                    Text(
                                        "Transcribe formulas, logic diagrams summaries, key definitions...",
                                        color = OnSurfaceDarker,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceText),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryTeal,
                                    unfocusedBorderColor = OnSurfaceDarker
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }

                    1 -> {
                        // TAB 2: AI CONTEXTUAL Q&A ASSISTANT PANEL
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Professor Deep-Dive Q&A",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                            }
                            Text(
                                "Ask follow-up questions about this topic based on the active lecture readings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceMuted
                            )

                            // Follow up log history scroll list
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (followUps.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No follow-up questions logged yet.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceDarker
                                            )
                                        }
                                    }
                                } else {
                                    items(followUps) { (q, ans) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(OnSurfaceDarker.copy(alpha = 0.15f))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.Top) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(q, fontWeight = FontWeight.SemiBold, color = OnSurfaceText, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(verticalAlignment = Alignment.Top) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(ans, color = OnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }

                                if (isFollowUpLoading) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryTeal, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Tutor explaining concept...", color = PrimaryTeal, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            // Dynamic follow up input bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = followUpInput,
                                    onValueChange = { followUpInput = it },
                                    placeholder = { Text("Query AI professor...", color = OnSurfaceDarker, style = MaterialTheme.typography.bodySmall) },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceText),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryTeal,
                                        unfocusedBorderColor = OnSurfaceDarker
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        if (followUpInput.isNotBlank()) {
                                            viewModel.askFollowUpQuestion(topic, subject.name, followUpInput)
                                            followUpInput = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryTeal.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Query", tint = PrimaryTeal)
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 3: TUTORING EXPLANATION HISTORY LOG
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Session Readings History",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                            }
                            Text(
                                "Retain context from previous readings. Click any of the logs to revert the central desk content instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceMuted
                            )

                            if (explanationHistory.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No prior explanations compiled in this active session.", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDarker)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(explanationHistory) { (mode, content) ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(OnSurfaceDarker.copy(alpha = 0.12f))
                                                .clickable {
                                                    viewModel.setTeachingContentDirectly(content)
                                                }
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text(mode, fontWeight = FontWeight.Bold, color = PrimaryTeal, style = MaterialTheme.typography.labelSmall)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(content.take(120) + "...", style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CUSTOM STYLED TEXTBOOK MD MARKDOWN PARSER
// ==========================================
@Composable
fun FormattedTeachingContent(text: String) {
    val lines = text.split("\n")
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.substring(4).trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.substring(3).trim(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.substring(2).trim(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText,
                        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = PrimaryTeal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                        FormattedRichText(trimmed.substring(2).trim(), modifier = Modifier.weight(1f))
                    }
                }
                trimmed.startsWith("> ") || trimmed.startsWith("\"") -> {
                    val rawClean = if (trimmed.startsWith("> ")) trimmed.substring(2).trim() else trimmed
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OnSurfaceDarker.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CORE DEFINITION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            Spacer(modifier = Modifier.height(4.dp))
                            FormattedRichText(rawClean)
                        }
                    }
                }
                trimmed.startsWith("```") -> {
                    // Let's filter out language headers, we keep code readable
                }
                trimmed.isNotEmpty() -> {
                    FormattedRichText(trimmed)
                }
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun FormattedRichText(text: String, modifier: Modifier = Modifier) {
    val parts = text.split("**")
    if (parts.size <= 1) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceText, modifier = modifier)
        return
    }

    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryTeal)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
    Text(annotatedString, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceText, modifier = modifier)
}

private fun getTabIdx(mode: String): Int {
    return when (mode.lowercase()) {
        "simple explanation" -> 0
        "beginner-friendly teaching" -> 1
        "detailed concept teaching" -> 2
        "exam-oriented teaching" -> 3
        "quick revision mode" -> 4
        "real-world analogy mode" -> 5
        "step-by-step breakdown mode" -> 6
        "concept reinforcement mode" -> 7
        "last-minute exam preparation mode" -> 8
        "active recall teaching mode" -> 9
        else -> 0
    }
}
