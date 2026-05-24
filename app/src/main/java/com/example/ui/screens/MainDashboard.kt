package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.Semester
import com.example.data.db.Subject
import com.example.data.db.Topic
import com.example.data.db.UploadedFile
import com.example.ui.theme.*
import com.example.ui.viewmodels.SemesterWorkspaceSection
import com.example.ui.viewmodels.StudyScreen
import com.example.ui.viewmodels.StudyViewModel
import com.example.ui.viewmodels.StudyMode
import com.example.ui.viewmodels.TopicRecommendation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudyDashboard(viewModel: StudyViewModel) {
    val semesters by viewModel.semesters.collectAsState()
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeSection by viewModel.activeSection.collectAsState()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (currentScreen != StudyScreen.Dashboard) {
        // Flat routing for secondary screens
        WorkspaceRouter(viewModel, screen = currentScreen)
        return
    }

    if (selectedSemester == null) {
        // Setup state configuration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSlateBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                title = "Set Up Your Academic Life",
                description = "Let's configure your semesters, dates, and syllabus outlines to start smart studying with your AI study companion.",
                buttonText = "Configure Semester",
                onClick = { viewModel.navigateTo(StudyScreen.SetupWorkspace) }
            )
        }
        return
    }

    val scaffoldContent = @Composable { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkSlateBg)
        ) {
            when (activeSection) {
                SemesterWorkspaceSection.Overview -> OverviewWorkspace(viewModel)
                SemesterWorkspaceSection.Subjects -> SubjectsWorkspace(viewModel)
                SemesterWorkspaceSection.Revision -> RevisionWorkspace(viewModel)
                SemesterWorkspaceSection.RecallPractice -> RecallPracticeWorkspace(viewModel)
                SemesterWorkspaceSection.UploadedMaterials -> UploadedMaterialsWorkspace(viewModel)
                SemesterWorkspaceSection.ProgressAnalytics -> ProgressAnalyticsWorkspace(viewModel)
                SemesterWorkspaceSection.AiAssistant -> AiAssistantWorkspace(viewModel)
            }
        }
    }

    if (isTablet) {
        // Desktop / Tablet Sidebar Layout
        Row(modifier = Modifier.fillMaxSize().background(DarkSlateBg)) {
            Sidebar(
                semesters = semesters,
                selectedSemester = selectedSemester,
                activeSection = activeSection,
                viewModel = viewModel,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(SurfaceDark)
            )
            VerticalDivider(
                thickness = 1.dp,
                color = OnSurfaceDarker.copy(alpha = 0.4f)
            )
            Box(modifier = Modifier.weight(1f)) {
                scaffoldContent(PaddingValues(0.dp))
            }
        }
    } else {
        // Mobile layout with modern responsive drawer sheet navigation
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = SurfaceDark,
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    Sidebar(
                        semesters = semesters,
                        selectedSemester = selectedSemester,
                        activeSection = activeSection,
                        viewModel = viewModel,
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        onItemClick = {
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    selectedSemester?.name ?: "Academic Companion",
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                                Text(
                                    activeSection.name,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    color = PrimaryTeal
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryTeal)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                    )
                },
                content = scaffoldContent
            )
        }
    }
}

@Composable
fun Sidebar(
    semesters: List<Semester>,
    selectedSemester: Semester?,
    activeSection: SemesterWorkspaceSection,
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {}
) {
    var showSemesterDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Global App Header Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Study Echo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceText
            )
        }

        // Active Workspace Select Dropdown Selector
        Text(
            text = "Active Cycle",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )

        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(OnSurfaceDarker.copy(alpha = 0.3f))
                    .clickable { showSemesterDropdown = !showSemesterDropdown }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = selectedSemester?.name ?: "Select Workspace",
                    color = OnSurfaceText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = OnSurfaceMuted)
            }

            DropdownMenu(
                expanded = showSemesterDropdown,
                onDismissRequest = { showSemesterDropdown = false },
                modifier = Modifier.background(SurfaceDark).width(248.dp)
            ) {
                semesters.forEach { sem ->
                    DropdownMenuItem(
                        text = { Text(sem.name, color = OnSurfaceText) },
                        onClick = {
                            viewModel.selectSemester(sem)
                            showSemesterDropdown = false
                            onItemClick()
                        },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryTeal) }
                    )
                }
                Divider(color = OnSurfaceDarker)
                DropdownMenuItem(
                    text = { Text("Configure New Cycle", color = PrimaryTeal, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        viewModel.navigateTo(StudyScreen.SetupWorkspace)
                        showSemesterDropdown = false
                        onItemClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryTeal) }
                )
            }
        }

        // Sidebar Navigation links
        Text(
            text = "Study Environment Modules",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        val navItems = listOf(
            NavigationLinkItem(SemesterWorkspaceSection.Overview, "Overview", Icons.Default.Dashboard),
            NavigationLinkItem(SemesterWorkspaceSection.Subjects, "Subjects", Icons.Default.AutoStories),
            NavigationLinkItem(SemesterWorkspaceSection.Revision, "Revision Hub", Icons.Default.RotateLeft),
            NavigationLinkItem(SemesterWorkspaceSection.RecallPractice, "Recall Practice", Icons.Default.Extension),
            NavigationLinkItem(SemesterWorkspaceSection.UploadedMaterials, "Uploaded Materials", Icons.Default.FolderOpen),
            NavigationLinkItem(SemesterWorkspaceSection.ProgressAnalytics, "Progress Analytics", Icons.Default.TrendingUp),
            NavigationLinkItem(SemesterWorkspaceSection.AiAssistant, "AI Study Guide", Icons.Default.AutoAwesome)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(navItems) { item ->
                val isSelected = activeSection == item.section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryTeal.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            viewModel.selectSection(item.section)
                            onItemClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) PrimaryTeal else OnSurfaceMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) OnSurfaceText else OnSurfaceMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick System Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.navigateTo(StudyScreen.SetupWorkspace) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Provision New Environment", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceText)
        }
    }
}

data class NavigationLinkItem(
    val section: SemesterWorkspaceSection,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun WorkspaceRouter(viewModel: StudyViewModel, screen: StudyScreen) {
    when (screen) {
        is StudyScreen.Dashboard -> MainStudyDashboard(viewModel)
        is StudyScreen.SetupWorkspace -> SemesterSetupScreen(viewModel)
        is StudyScreen.SubjectDetail -> SubjectDetailScreen(viewModel)
        is StudyScreen.TutorWorkspace -> TutorWorkspaceScreen(viewModel)
        is StudyScreen.QuizWorkspace -> QuizWorkspaceScreen(viewModel)
        is StudyScreen.FlashcardWorkspace -> FlashcardWorkspaceScreen(viewModel)
        is StudyScreen.PreExamPrep -> PreExamPrepScreen(viewModel)
        is StudyScreen.RecallSession -> RecallSessionScreen(viewModel)
    }
}

// ==========================================
// 1. OVERVIEW SECTION (Semester Dashboard)
// ==========================================
@Composable
fun OverviewWorkspace(viewModel: StudyViewModel) {
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val recommendation by viewModel.recommendation.collectAsState()
    val files by viewModel.uploadedFiles.collectAsState()

    val now = System.currentTimeMillis()
    val examDate = selectedSemester?.examDate ?: now
    val remainingDaysToExam = maxOf(0L, TimeUnit.MILLISECONDS.toDays(examDate - now))
    val totalDays = maxOf(1L, TimeUnit.MILLISECONDS.toDays((selectedSemester?.endDate ?: now) - (selectedSemester?.startDate ?: now)))
    val completedDays = maxOf(0L, TimeUnit.MILLISECONDS.toDays(now - (selectedSemester?.startDate ?: now)))
    val remainingDaysToSemesterEnd = maxOf(0L, (totalDays - completedDays))

    val averageProgress = if (subjects.isNotEmpty()) {
        subjects.map { it.completionProgress }.average().toInt()
    } else 0

    val weakSubjectsCount = subjects.filter { it.difficulty.lowercase() == "hard" || it.priority.lowercase() == "high" }.size

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Welcoming & Study Goals Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Good Day, Scholar",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceText
                            )
                            Text(
                                "Intelligent Academic Workspace Details",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryTeal
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$remainingDaysToSemesterEnd Days Left in Cycle",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Optional Study Goals & Vision", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        selectedSemester?.studyGoals ?: "No target goals entered yet. Expand your workspace configuration to persist milestones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceText
                    )
                }
            }
        }

        // Study Situation Modes Selector
        item {
            val activeMode by viewModel.currentStudyMode.collectAsState()
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        Color(activeMode.accentColorHex).copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Workspace Mode Adaptability", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = Color(activeMode.accentColorHex)
                    )
                    Text(
                        "Adapt the application workspace environment to prioritize specific study behaviors for different workloads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted
                    )

                    // Scrollable modes buttons row
                    ScrollableTabRow(
                        selectedTabIndex = StudyMode.values().indexOf(activeMode),
                        containerColor = DarkSlateBg,
                        contentColor = Color(activeMode.accentColorHex),
                        edgePadding = 8.dp,
                        divider = {},
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    ) {
                        StudyMode.values().forEach { mode ->
                            val isSel = mode == activeMode
                            Tab(
                                selected = isSel,
                                onClick = { viewModel.setStudyMode(mode) },
                                modifier = Modifier.testTag("study_mode_tab_${mode.name}"),
                                text = {
                                    Text(
                                        mode.label, 
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSel) Color(mode.accentColorHex) else OnSurfaceMuted
                                    )
                                }
                            )
                        }
                    }

                    // Adaptive explanation card based on selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(activeMode.accentColorHex).copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(activeMode.accentColorHex), modifier = Modifier.size(16.dp))
                                Text(
                                    activeMode.description, 
                                    style = MaterialTheme.typography.labelSmall, 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color(activeMode.accentColorHex)
                                )
                            }
                            Text(
                                activeMode.focusTip, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = OnSurfaceText
                            )
                        }
                    }
                }
            }
        }

        // --- Dynamic Academic Warnings & Smart Notifications Center ---
        item {
            val dueRev by viewModel.dueRevisionTopics.collectAsState()
            val weakTop by viewModel.weakestTopics.collectAsState()

            if (remainingDaysToExam <= 14 || dueRev.isNotEmpty() || weakTop.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AlertCrimson.copy(alpha = 0.25f))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertCrimson, modifier = Modifier.size(20.dp))
                            Text(
                                "Intelligent Notification & Warning Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceText
                            )
                        }

                        if (remainingDaysToExam <= 14) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AlertCrimson.copy(alpha = 0.08f)).padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = AlertCrimson, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Upcoming Exam Urgency Alert", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AlertCrimson)
                                    Text("Final syllabus milestones check matches high urgency! Shift focus coordinates immediately to Revision Mode or Survival Practice.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                                }
                            }
                        }

                        if (dueRev.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(TertiaryViolet.copy(alpha = 0.08f)).padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = TertiaryViolet, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Active Recall Deficit warning", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TertiaryViolet)
                                    Text("You have ${dueRev.size} scheduled topics requiring immediate memory reinforcement. Enter active revision or flashcard clinics soon.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                                }
                            }
                        }

                        if (weakTop.isNotEmpty()) {
                            val targetWeak = weakTop.first()
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(WarningAmber.copy(alpha = 0.08f)).padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Weak-Topic Recovery Suggestion", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = WarningAmber)
                                    Text("Your recorded retention score on '${targetWeak.name}' is low. AI Coach recommends scheduling immediate concept clarification sessions.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Analytical KPIs Grid Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KPIIndicatorCard(
                    title = "Course Modules",
                    value = "${subjects.size}",
                    subText = "Registered Core Modules",
                    icon = Icons.Default.AutoStories,
                    color = PrimaryTeal,
                    modifier = Modifier.weight(1f)
                )

                KPIIndicatorCard(
                    title = "Syllabus Progress",
                    value = "$averageProgress%",
                    subText = "Avg Course Progress",
                    icon = Icons.Default.DataUsage,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KPIIndicatorCard(
                    title = "Weak Subjects",
                    value = "$weakSubjectsCount",
                    subText = "High Difficulty Courses",
                    icon = Icons.Default.Warning,
                    color = WarningAmber,
                    modifier = Modifier.weight(1f)
                )

                KPIIndicatorCard(
                    title = "Uploaded Assets",
                    value = "${files.size} Files",
                    subText = "Indexed textbooks & notes",
                    icon = Icons.Default.GridOn,
                    color = SecondaryBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Smart Adaptive Recommendation
        item {
            RecommendationCard(recommendation, onStartStudying = { topic ->
                viewModel.selectTopic(topic)
            })
        }

        // AI study gaps & academic insights section
        item {
            val aiInsights by viewModel.aiInsights.collectAsState()
            if (aiInsights.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "AI Academic Gaps & Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                    
                    aiInsights.forEach { insight ->
                        val borderTint = when (insight.statusLevel.lowercase()) {
                            "critical" -> AlertCrimson
                            "warning" -> WarningAmber
                            "good" -> SuccessGreen
                            else -> PrimaryTeal
                        }
                        
                        val icon = when (insight.statusLevel.lowercase()) {
                            "critical" -> Icons.Default.ReportProblem
                            "warning" -> Icons.Default.TrendingDown
                            "good" -> Icons.Default.TrendingUp
                            else -> Icons.Default.Psychology
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderTint.copy(alpha = 0.4f))),
                            modifier = Modifier.fillMaxWidth().testTag("ai_insight_${insight.title.replace(" ", "_").lowercase()}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(icon, contentDescription = null, tint = borderTint, modifier = Modifier.size(20.dp))
                                        Text(
                                            insight.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurfaceText
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(borderTint.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Retention: ${insight.estimatedExamReadyPercent}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = borderTint,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Text(
                                    insight.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceMuted
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(borderTint.copy(alpha = 0.05f))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            "AI Suggested Recovery Action Plan:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = borderTint
                                        )
                                        Text(
                                            insight.suggestedActionPlan,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Exam Countdown details
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
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AlertCrimson.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = AlertCrimson, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upcoming Milestone Final Exams",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceText
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date(examDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$remainingDaysToExam",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AlertCrimson
                        )
                        Text(
                            text = "days left",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KPIIndicatorCard(
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)
            Text(subText, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
        }
    }
}

// ==========================================
// 2. SUBJECTS LISTING & MANAGEMENT
// ==========================================
@Composable
fun SubjectsWorkspace(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    var isAddingSubject by remember { mutableStateOf(false) }

    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectCode by remember { mutableStateOf("") }
    var newSubjectDiff by remember { mutableStateOf("Medium") }
    var newSubjectPriority by remember { mutableStateOf("Medium") }
    var completionProgress by remember { mutableStateOf(0f) }

    val colors = listOf(
        0xFF3B82F6.toInt(), // Blue
        0xFF10B981.toInt(), // Success Emerald
        0xFF8B5CF6.toInt(), // Indigo
        0xFFEF4444.toInt(), // Target Crimson
        0xFFF59E0B.toInt(), // Amber
        0xFF06B6D4.toInt()  // Cyan
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Semester Course Modules",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                    Text(
                        "Manage your core academic subjects and syllabus metrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted
                    )
                }

                Button(
                    onClick = { isAddingSubject = !isAddingSubject },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(if (isAddingSubject) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAddingSubject) "Collapse" else "New Subject")
                }
            }
        }

        if (isAddingSubject) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Provision New Course Module", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryTeal)

                        OutlinedTextField(
                            value = newSubjectName,
                            onValueChange = { newSubjectName = it },
                            label = { Text("Subject / Course Name", color = OnSurfaceMuted) },
                            textStyle = LocalTextStyle.current.copy(color = OnSurfaceText),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = newSubjectCode,
                                onValueChange = { newSubjectCode = it },
                                label = { Text("Code (e.g. CS-201)", color = OnSurfaceMuted) },
                                textStyle = LocalTextStyle.current.copy(color = OnSurfaceText),
                                modifier = Modifier.weight(1f)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Select Course Color Decor", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    colors.take(4).forEach { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(hex))
                                                .clickable { /* Select Decor Color action */ }
                                        )
                                    }
                                }
                            }
                        }

                        // Difficulty Segment chip group
                        Column {
                            Text("Difficulty Level Assessment", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Easy", "Medium", "Hard").forEach { d ->
                                    FilterChip(
                                        selected = newSubjectDiff == d,
                                        onClick = { newSubjectDiff = d },
                                        label = { Text(d) }
                                    )
                                }
                            }
                        }

                        // Priority rating Segment chip group
                        Column {
                            Text("Execution Priority Rating", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Low", "Medium", "High").forEach { pr ->
                                    FilterChip(
                                        selected = newSubjectPriority == pr,
                                        onClick = { newSubjectPriority = pr },
                                        label = { Text(pr) }
                                    )
                                }
                            }
                        }

                        // Completion assessment progress slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated Syllabus Progress Code", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                                Text("${completionProgress.toInt()}%", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = completionProgress,
                                onValueChange = { completionProgress = it },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryTeal,
                                    activeTrackColor = PrimaryTeal
                                )
                            )
                        }

                        Button(
                            onClick = {
                                if (newSubjectName.isNotBlank() && newSubjectCode.isNotBlank()) {
                                    viewModel.createSubject(
                                        name = newSubjectName,
                                        code = newSubjectCode,
                                        color = colors.random(),
                                        difficulty = newSubjectDiff,
                                        priority = newSubjectPriority,
                                        completionProgress = completionProgress.toInt()
                                    )
                                    newSubjectName = ""
                                    newSubjectCode = ""
                                    isAddingSubject = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Add Course to Workspace", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            if (subjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No subjects registered inside this semester workspace yet.", color = OnSurfaceMuted)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(280.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 1000.dp)
                ) {
                    items(subjects) { subject ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSubject(subject)
                                    viewModel.navigateTo(StudyScreen.SubjectDetail)
                                }
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(subject.color).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(subject.subjectCode, color = Color(subject.color), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (subject.difficulty.lowercase()) {
                                                        "hard" -> AlertCrimson.copy(alpha = 0.15f)
                                                        "medium" -> WarningAmber.copy(alpha = 0.15f)
                                                        else -> SuccessGreen.copy(alpha = 0.15f)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                subject.difficulty,
                                                color = when (subject.difficulty.lowercase()) {
                                                    "hard" -> AlertCrimson
                                                    "medium" -> WarningAmber
                                                    else -> SuccessGreen
                                                },
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SecondaryBlue.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Priority: ${subject.priority}", color = SecondaryBlue, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Text(
                                    subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Syllabus Completion", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                                        Text("${subject.completionProgress}%", style = MaterialTheme.typography.labelSmall, color = OnSurfaceText, fontWeight = FontWeight.SemiBold)
                                    }

                                    LinearProgressIndicator(
                                        progress = subject.completionProgress / 100f,
                                        color = Color(subject.color),
                                        trackColor = OnSurfaceDarker,
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(100))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Enter Workspace", color = PrimaryTeal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
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
// 3. REVISION SCHEDULES HUB
// ==========================================
@Composable
fun RevisionWorkspace(viewModel: StudyViewModel) {
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val dueRevisionTopics by viewModel.dueRevisionTopics.collectAsState()
    val weakestTopics by viewModel.weakestTopics.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    "Spaced Repetition & Revision Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    "Adaptive system algorithms plotting target learning trajectories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryTeal.copy(alpha = 0.3f))),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryTeal)
                        Text("Active SM-2 Spaced Repetition Scheduling Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                    }
                    Text(
                        "The system logs student retention levels (Forgotten, Difficult, Medium, Easy) during active recall sessions and adaptively schedules future reviews to maximize long-term consolidation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.35f
                    )
                }
            }
        }

        // Section for overdue topics
        item {
            Text(
                text = "My Revision Backlog (${dueRevisionTopics.size} Overdue Chapters)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceText
            )
        }

        if (dueRevisionTopics.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Text("All Caught Up!", fontWeight = FontWeight.Bold, color = OnSurfaceText)
                        Text(
                            "You have no topics overdue for review currently. Great job keeping up with your spaced memory targets!",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(dueRevisionTopics) { topic ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(topic.name, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val masteryLabel = getTopicMasteryText(topic)
                                val masteryColor = getTopicMasteryLabel(topic)

                                Surface(shape = RoundedCornerShape(6.dp), color = masteryColor.copy(alpha = 0.15f)) {
                                    Text(
                                        text = masteryLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = masteryColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("⏱️ ${topic.estimatedStudyTimeMinutes}m", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.startRecallSession(null, listOf(topic))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("hub_recall_${topic.id}")
                        ) {
                            Text("Challenge", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Section for weakest topics needing recovery
        if (weakestTopics.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Vulnerable Areas Needing Revision",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AlertCrimson
                )
            }

            items(weakestTopics.take(3)) { topic ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(topic.name, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                            Text("Weakness Score: ${(topic.weakScore * 100).toInt()}% • Intervals: ${topic.revisionFrequency} reviews", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                        }

                        IconButton(
                            onClick = {
                                viewModel.selectTopic(topic)
                            }
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Active Study", tint = PrimaryTeal)
                        }
                    }
                }
            }
        }

        // Section for Semantic AI Study Recommendations
        item {
            val semanticRecommendations by viewModel.semanticRecommendations.collectAsState()
            val subjects by viewModel.subjects.collectAsState()
            if (semanticRecommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "AI-Driven Priority Graph Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    
                    semanticRecommendations.forEach { rec ->
                        val subName = subjects.find { it.id == rec.topic.subjectId }?.name ?: "Course"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(PrimaryTeal.copy(alpha = 0.25f))
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("semantic_rec_${rec.topic.name.replace(" ", "_").lowercase()}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            rec.topic.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurfaceText
                                        )
                                        Text(
                                            "Subject: $subName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OnSurfaceMuted
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PrimaryTeal.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Priority: ${rec.priorityScore}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryTeal,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Text(
                                    rec.reasonPhasing,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceMuted
                                )
                                
                                if (rec.learningSyllabusContext.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Context: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OnSurfaceMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(OnSurfaceDarker)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(rec.learningSyllabusContext, style = MaterialTheme.typography.labelSmall, color = OnSurfaceText)
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
// 4. ACTIVE RECALL PRACTICE CLINIC
// ==========================================
@Composable
fun RecallPracticeWorkspace(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    "Recall Practice & Quizzes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    "Challenge your memory index using active recall cards and multiple-choice quizzes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(36.dp))
                    Text("Interactive Recall & Flashcards Clinic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                    Text(
                        "Choose a registered subject workspace below and enter the dynamic learning room to trigger instant multiple choice quizzes or index card revision pools.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted
                    )
                }
            }
        }

        items(subjects) { subject ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .clickable {
                        viewModel.selectSubject(subject)
                        viewModel.navigateTo(StudyScreen.SubjectDetail)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(subject.color).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = Color(subject.color))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                    Text("${subject.subjectCode} • Difficulty: ${subject.difficulty}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                }

                Button(
                    onClick = {
                        viewModel.startRecallSessionForSubject(subject)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnSurfaceDarker, contentColor = PrimaryTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("run_recall_session_of_${subject.id}")
                ) {
                    Text("Run Recall Session", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 5. UPLOADED MATERIALS PER SUBJECT
// ==========================================
@Composable
fun UploadedMaterialsWorkspace(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val files by viewModel.uploadedFiles.collectAsState()
    val progress by viewModel.uploadProgress.collectAsState()

    var selectedSubjectFilter by remember { mutableStateOf<Subject?>(null) }
    var dragDemoActive by remember { mutableStateOf(false) }

    val filteredFiles = if (selectedSubjectFilter == null) files else files.filter { it.subjectId == selectedSubjectFilter?.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    "Materials & Resource Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    "Upload syllabus specifications, lecture PDFs, worksheets, or notebook scans",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted
                )
            }
        }

        // Drag and drop responsive simulator dropzone
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (dragDemoActive) PrimaryTeal.copy(alpha = 0.08f) else SurfaceDark
                ),
                border = BorderStroke(1.5.dp, if (dragDemoActive) PrimaryTeal else OnSurfaceDarker),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dragDemoActive = !dragDemoActive }
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (dragDemoActive) PrimaryTeal else OnSurfaceMuted,
                        modifier = Modifier.size(44.dp)
                    )

                    Text(
                        if (dragDemoActive) "Release files to upload directly!" else "File Upload Dropzone & Drag Simulator",
                        fontWeight = FontWeight.Bold,
                        color = if (dragDemoActive) PrimaryTeal else OnSurfaceText
                    )

                    Text(
                        "Click to simulate dragging & dropping textbook elements, syllabus screenshots, or handwritten notes lists here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )

                    if (dragDemoActive) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            listOf(
                                Triple("Algorithm_Manual.pdf", "PDF", "4.2 MB"),
                                Triple("Network_Topologies.png", "Image", "1.1 MB"),
                                Triple("Handwritten_Linear_Algebra.jpg", "Handwritten Notes", "800 KB")
                            ).forEach { (fname, type, size) ->
                                Button(
                                    onClick = {
                                        val activeSub = subjects.firstOrNull()
                                        if (activeSub != null) {
                                            viewModel.uploadSubjectFile(activeSub.id, fname, type, size)
                                            dragDemoActive = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Drop $fname", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                                }
                            }
                        }
                    }

                    progress?.let { pr ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uploading & Indexing Semantic Tree...", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal)
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

        // Sorting / filtering files tab slider
        item {
            ScrollableTabRow(
                selectedTabIndex = if (selectedSubjectFilter == null) 0 else subjects.indexOf(selectedSubjectFilter) + 1,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                Tab(
                    selected = selectedSubjectFilter == null,
                    onClick = { selectedSubjectFilter = null },
                    text = { Text("All Subjects", color = if (selectedSubjectFilter == null) PrimaryTeal else OnSurfaceMuted) }
                )

                subjects.forEach { sub ->
                    Tab(
                        selected = selectedSubjectFilter?.id == sub.id,
                        onClick = { selectedSubjectFilter = sub },
                        text = { Text(sub.name, color = if (selectedSubjectFilter?.id == sub.id) Color(sub.color) else OnSurfaceMuted) }
                    )
                }
            }
        }

        // Indexed Vault listing
        if (filteredFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No resource files uploaded in this filter tab yet.", color = OnSurfaceMuted)
                }
            }
        } else {
            items(filteredFiles) { file ->
                val parentSubject = subjects.find { it.id == file.subjectId }
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
                                        else -> Icons.Default.NoteAlt
                                    },
                                    contentDescription = null,
                                    tint = parentSubject?.let { Color(it.color) } ?: PrimaryTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(file.name, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                                    Text("Resource Category: ${file.fileType} • Size: ${file.fileSize}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteUploadedFile(file) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete resource", tint = AlertCrimson.copy(alpha = 0.8f))
                            }
                        }

                        // Display extracted layout structures/chapters extracted beforehand
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OnSurfaceDarker.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Insights, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI File Index Preview Layout (Topic Structure)", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    file.extractedChaptersText ?: "Preparing schema tree...",
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

// ==========================================
// 6. PROGRESS ANALYTICS SPEEDOMETER & CHARTS
// ==========================================
@Composable
fun ProgressAnalyticsWorkspace(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    "Progress Analytics & Readiness",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    "Syllabus tracking analytics matching your exam readiness quotient",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted
                )
            }
        }

        // Custom hand-drawn visual graph card representing estimated readiness quotient
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Overall Syllabus Exam Readiness speedometer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)

                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Simulated speedometer circle overlay
                        CircularProgressIndicator(
                            progress = 0.72f,
                            modifier = Modifier.fillMaxSize(),
                            color = PrimaryTeal,
                            strokeWidth = 12.dp,
                            trackColor = OnSurfaceDarker.copy(alpha = 0.4f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("72%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryTeal)
                            Text("Optimal Zone", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "Speedometer calibrated from completion ratios, spaced recall retention intervals, and active flashcards revision consistency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Subject Coverage Comparison Grid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceText)

                    subjects.forEach { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.name, color = OnSurfaceText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${s.completionProgress}% coverage", color = Color(s.color), style = MaterialTheme.typography.labelSmall)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = s.completionProgress / 100f,
                                    color = Color(s.color),
                                    trackColor = OnSurfaceDarker,
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(100))
                                )
                                Text(
                                    text = if (s.completionProgress >= 60) "Robust" else "Struggled",
                                    color = if (s.completionProgress >= 60) SuccessGreen else WarningAmber,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. AI STUDY ASSISTANT & STRATEGIC RECOMMENDATIONS
// ==========================================
@Composable
fun AiAssistantWorkspace(viewModel: StudyViewModel) {
    var promptInput by remember { mutableStateOf("") }
    var aiLogs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var aiLoading by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    "AI Academic Coach & Paths",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    "Strategy planner and companion tutor to optimize exam efficiency",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted
                )
            }
        }

        // Prompt input console
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Study Goal Strategist Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OnSurfaceText)
                    }

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("E.g., Generate a customized 4-week study path to master linear algebra.", color = OnSurfaceDarker) },
                        textStyle = LocalTextStyle.current.copy(color = OnSurfaceText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = OnSurfaceDarker
                        ),
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    )

                    Button(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                aiLoading = true
                                val userPmp = promptInput
                                promptInput = ""
                                // Simulate responsive response delay
                                aiLogs = aiLogs + Pair("user", userPmp)
                                val responseText = """
                                    Here is a tailored roadmap based on your active semester targets:
                                    • Week 1: Spend 4 hours reviewing vector bases & matrix decompositions. Follow up with recall quiz 1.
                                    • Week 2: Dive into Orthogonality & Projection matrix layers. Register detailed notes inside Tutor Workspace.
                                    • Week 3: Master Eigenvalues & Spectral decompositions focusing on high-difficulty priority markers.
                                    • Week 4: Finalize coverage. Record flashcards and run recall runs to reduce weakness metrics.
                                """.trimIndent()

                                aiLogs = aiLogs + Pair("assistant", responseText)
                                aiLoading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Construct Study Path", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (aiLogs.isNotEmpty()) {
            items(aiLogs) { (sender, msg) ->
                val isAssistant = sender == "assistant"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAssistant) SurfaceDark else PrimaryTeal.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                if (isAssistant) "AI Coach" else "Scholar Student",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAssistant) PrimaryTeal else OnSurfaceText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg, color = OnSurfaceText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    buttonText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceText
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted,
                textAlign = TextAlign.Center
            )

            if (buttonText != null && onClick != null) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg)
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: TopicRecommendation?,
    onStartStudying: (Topic) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryTeal.copy(alpha = 0.4f))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "What Should I Study Now? Advisor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                }

                recommendation?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AlertCrimson.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Exam in ${it.remainingDaysToExam} Days",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertCrimson,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (recommendation == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(OnSurfaceDarker),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = OnSurfaceMuted)
                    }
                    Column {
                        Text(
                            text = "AI Advisor is analyzing...",
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceText
                        )
                        Text(
                            text = "Introduce course nodes or study subjects into the database to unlock priorities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // AI Mentor Avatar Frame
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryTeal.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(28.dp))
                            Text("ECHO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryTeal, fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f)
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Subject Context: ${recommendation.subjectName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = recommendation.topic.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceText
                        )
                        Text(
                            text = recommendation.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // High-fidelity actionable buttons
                Button(
                    onClick = { onStartStudying(recommendation.topic) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = DarkSlateBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_study_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Active Revision Space", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
