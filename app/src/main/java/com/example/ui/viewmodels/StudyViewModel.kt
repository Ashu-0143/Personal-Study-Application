package com.example.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.*
import kotlinx.coroutines.flow.combine
import com.example.data.db.AppDatabase
import com.example.data.db.Flashcard
import com.example.data.db.Semester
import com.example.data.db.Subject
import com.example.data.db.Topic
import com.example.data.db.UploadedFile
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class SemesterWorkspaceSection {
    Overview,
    Subjects,
    Revision,
    RecallPractice,
    UploadedMaterials,
    ProgressAnalytics,
    AiAssistant
}

enum class StudyMode(
    val label: String,
    val description: String,
    val accentColorHex: Long,
    val focusTip: String
) {
    DEEP_STUDY("Deep Study", "Detailed reading and initial understanding.", 0xFF0D9488, "Turn off off-task notifications. Read concept explanations, outline maps, and formulate your first summaries."),
    REVISION("Revision Mode", "Active content review and fast re-learning.", 0xFF8B5CF6, "Focus on summarizing topics rapidly. Re-study structural syllabus nodes to prepare for active recall."),
    RECALL_CHALLENGE("Recall Challenge", "Active retrieval practice and retention grading.", 0xFF3B82F6, "No re-reading allowed! View questions, retrieve the concept in your head, then reveal the answer."),
    EXAM_SURVIVAL("Exam Survival", "High-efficiency study under extreme urgency.", 0xFFEF4444, "Avoid minor/low-weight topics. Drill high-importance concepts and immediate weak spots."),
    LAST_MINUTE("Last-Minute Prep", "Formulas, bullet-cheats, and condensed cram-sheets.", 0xFFF59E0B, "Review condensed AI summaries. Relax. 7-8 hours of sleep boosts your score by up to 20%!")
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository

    // Current Study Mode
    private val _currentStudyMode = MutableStateFlow(StudyMode.DEEP_STUDY)
    val currentStudyMode: StateFlow<StudyMode> = _currentStudyMode.asStateFlow()

    fun setStudyMode(mode: StudyMode) {
        _currentStudyMode.value = mode
    }

    // Active screen navigation
    private val _currentScreen = MutableStateFlow<StudyScreen>(StudyScreen.Dashboard)
    val currentScreen: StateFlow<StudyScreen> = _currentScreen.asStateFlow()

    private val _activeSection = MutableStateFlow(SemesterWorkspaceSection.Overview)
    val activeSection: StateFlow<SemesterWorkspaceSection> = _activeSection.asStateFlow()

    // Database states
    val semesters: StateFlow<List<Semester>>

    private val _selectedSemester = MutableStateFlow<Semester?>(null)
    val selectedSemester: StateFlow<Semester?> = _selectedSemester.asStateFlow()

    val subjects: StateFlow<List<Subject>>
    private val _selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedSubject: StateFlow<Subject?> = _selectedSubject.asStateFlow()

    val topics: StateFlow<List<Topic>>
    private val _selectedTopic = MutableStateFlow<Topic?>(null)
    val selectedTopic: StateFlow<Topic?> = _selectedTopic.asStateFlow()

    // Uploaded materials state
    val uploadedFiles: StateFlow<List<UploadedFile>>
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    // Due Revision & Weakest Topics lists
    val dueRevisionTopics: StateFlow<List<Topic>>
    val weakestTopics: StateFlow<List<Topic>>

    // Intelligent AI Academic Insights & Semantic Recommendations
    val aiInsights: StateFlow<List<AiAcademicInsight>>
    val semanticRecommendations: StateFlow<List<SemanticRecommendation>>

    // Explanation & Tutoring states
    private val _teachingContent = MutableStateFlow<String?>(null)
    val teachingContent: StateFlow<String?> = _teachingContent.asStateFlow()

    private val _isTeachingLoading = MutableStateFlow(false)
    val isTeachingLoading: StateFlow<Boolean> = _isTeachingLoading.asStateFlow()

    private val _selectedExplanationMode = MutableStateFlow("Simple Explanation")
    val selectedExplanationMode: StateFlow<String> = _selectedExplanationMode.asStateFlow()

    // Local state for active tutoring panel notes & history
    private val _explanationHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val explanationHistory: StateFlow<List<Pair<String, String>>> = _explanationHistory.asStateFlow()

    private val _topicNotes = MutableStateFlow<Map<Int, String>>(emptyMap())
    val topicNotes: StateFlow<Map<Int, String>> = _topicNotes.asStateFlow()

    // Follow-up interaction states
    private val _followUps = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val followUps: StateFlow<List<Pair<String, String>>> = _followUps.asStateFlow()

    private val _isFollowUpLoading = MutableStateFlow(false)
    val isFollowUpLoading: StateFlow<Boolean> = _isFollowUpLoading.asStateFlow()

    // Quiz states
    private val _activeQuiz = MutableStateFlow<List<StudyQuizQuestion>?>(null)
    val activeQuiz: StateFlow<List<StudyQuizQuestion>?> = _activeQuiz.asStateFlow()

    private val _isQuizLoading = MutableStateFlow(false)
    val isQuizLoading: StateFlow<Boolean> = _isQuizLoading.asStateFlow()

    // Flashcard states
    val topicFlashcards: StateFlow<List<Flashcard>>
    private val _isGeneratingFlashcards = MutableStateFlow(false)
    val isGeneratingFlashcards: StateFlow<Boolean> = _isGeneratingFlashcards.asStateFlow()

    // Recommended learning state ("What Should I Study Now?")
    private val _recommendation = MutableStateFlow<TopicRecommendation?>(null)
    val recommendation: StateFlow<TopicRecommendation?> = _recommendation.asStateFlow()

    // Condensed Exam Prep Sheet
    private val _condensedPrepContent = MutableStateFlow<String?>(null)
    val condensedPrepContent: StateFlow<String?> = _condensedPrepContent.asStateFlow()

    private val _isPrepLoading = MutableStateFlow(false)
    val isPrepLoading: StateFlow<Boolean> = _isPrepLoading.asStateFlow()

    // --- Active Recall Session Engine ---
    data class ActiveRecallQuestion(
        val id: Int,
        val prompt: String,
        val detailQuestion: String,
        val suggestedAnswer: String,
        val categoryType: String, // Concept Comparison, Short Answer, Quick Check, Exam Prompt
        val associatedTopic: Topic
    )

    private val _activeRecallQuestions = MutableStateFlow<List<ActiveRecallQuestion>>(emptyList())
    val activeRecallQuestions: StateFlow<List<ActiveRecallQuestion>> = _activeRecallQuestions.asStateFlow()

    fun startRecallSessionForSubject(subject: Subject) {
        viewModelScope.launch {
            val subTopics = repository.getTopicsForSubjectSync(subject.id)
            if (subTopics.isNotEmpty()) {
                startRecallSession(subject, subTopics)
            } else {
                val sampleTopics = listOf(
                    Topic(id = 9991 + subject.id, subjectId = subject.id, name = "Introduction and Core Fundamentals", orderIndex = 0, summary = "Fundamentals of study materials and subject context."),
                    Topic(id = 9992 + subject.id, subjectId = subject.id, name = "Advanced Applied Methodologies", orderIndex = 1, summary = "Applying theoretical structures under practical exam constraints.")
                )
                startRecallSession(subject, sampleTopics)
            }
        }
    }

    private val _currentRecallIndex = MutableStateFlow(0)
    val currentRecallIndex: StateFlow<Int> = _currentRecallIndex.asStateFlow()

    private val _recallCompletedScores = MutableStateFlow<List<Pair<ActiveRecallQuestion, String>>>(emptyList())
    val recallCompletedScores: StateFlow<List<Pair<ActiveRecallQuestion, String>>> = _recallCompletedScores.asStateFlow()

    fun startRecallSession(forSubject: Subject?, callbackTopics: List<Topic>) {
        viewModelScope.launch {
            if (callbackTopics.isEmpty()) return@launch
            val questions = callbackTopics.flatMapIndexed { index, topic ->
                listOf(
                    ActiveRecallQuestion(
                        id = index * 2,
                        prompt = "Key Core Concept Check on ${topic.name}",
                        detailQuestion = "Explain the fundamental principles, key details, and importance of ${topic.name}.",
                        suggestedAnswer = topic.summary ?: "This chapter covers crucial exam components. Ensure you understand its mathematical structures and core definitions.",
                        categoryType = "Concept Comparison",
                        associatedTopic = topic
                    ),
                    ActiveRecallQuestion(
                        id = index * 2 + 1,
                        prompt = "Exam Practice Assessment for ${topic.name}",
                        detailQuestion = "If you were asked to analyze a case study or design a system utilizing ${topic.name}, what would be the standard workflow and 3 major pitfalls?",
                        suggestedAnswer = "Workflow: 1. Scoping objectives. 2. Applying structured formulas or structural constraints. 3. Reviewing results. Pitfalls: Insufficient parameters, over-optimization, ignoring edge cases.",
                        categoryType = "Exam-Style Question",
                        associatedTopic = topic
                    )
                )
            }.shuffled().take(6) // Pick 6 dynamic questions for a rapid practice session!

            _activeRecallQuestions.value = questions
            _currentRecallIndex.value = 0
            _recallCompletedScores.value = emptyList()
            if (forSubject != null) {
                _selectedSubject.value = forSubject
            }
            _currentScreen.value = StudyScreen.RecallSession
        }
    }

    fun submitRecallRating(question: ActiveRecallQuestion, ratingLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ratingScore = when (ratingLabel) {
                "Forgotten" -> 1
                "Difficult" -> 2
                "Medium" -> 2
                "Needs Revision" -> 1
                "Easy" -> 3
                else -> 3
            }
            repository.recordStudySession(question.associatedTopic, ratingScore)
            
            val updatedTopic = question.associatedTopic.copy(
                revisionFrequency = question.associatedTopic.revisionFrequency + 1,
                needsRevision = (ratingScore == 1 || ratingLabel == "Needs Revision")
            )
            repository.updateTopic(updatedTopic)

            withContext(Dispatchers.Main) {
                _recallCompletedScores.value = _recallCompletedScores.value + (question to ratingLabel)
                if (_currentRecallIndex.value < _activeRecallQuestions.value.size - 1) {
                    _currentRecallIndex.value = _currentRecallIndex.value + 1
                } else {
                    // Session complete! Keep indices as is for final recap block.
                }
            }
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyRepository(database.studyDao())

        semesters = repository.allSemesters.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactively load subjects when semester changes
        subjects = _selectedSemester.flatMapLatest { semester ->
            semester?.let { repository.getSubjectsForSemester(it.id) } ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactively load topics when subject changes
        topics = _selectedSubject.flatMapLatest { subject ->
            subject?.let { repository.getTopicsForSubject(it.id) } ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactively load flashcards when topic changes
        topicFlashcards = _selectedTopic.flatMapLatest { topic ->
            topic?.let { repository.getFlashcardsForTopic(it.id) } ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactively load uploaded materials
        uploadedFiles = _selectedSemester.flatMapLatest { semester ->
            semester?.let { repository.getAllUploadedFilesForSemester(it.id) } ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactively load revision schedules and weak spots
        dueRevisionTopics = _selectedSemester.flatMapLatest { semester ->
            semester?.let { repository.getDueRevisionTopics(it.id) } ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        weakestTopics = _selectedSemester.flatMapLatest { semester ->
            semester?.let { repository.getWeakestTopics(it.id) } ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactively compute educational insights and study recommendations using AI services
        aiInsights = combine(
            subjects,
            _selectedSemester,
            dueRevisionTopics,
            weakestTopics
        ) { subs, sem, due, weak ->
            if (sem == null) emptyList()
            else {
                val allTopics = subs.flatMap { repository.getTopicsForSubjectSync(it.id) }
                val remainingDaysToExam = maxOf(1, TimeUnit.MILLISECONDS.toDays(sem.examDate - System.currentTimeMillis()).toInt())
                AiStudyInsightsEngine.generateIntelligentStudyInsights(
                    subjects = subs,
                    allTopics = allTopics,
                    dueTopics = due,
                    weakTopics = weak,
                    targetExamDays = remainingDaysToExam
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        semanticRecommendations = combine(
            subjects,
            _selectedSemester,
            dueRevisionTopics,
            weakestTopics
        ) { subs, sem, due, weak ->
            if (sem == null) emptyList()
            else {
                val allTopics = subs.flatMap { repository.getTopicsForSubjectSync(it.id) }
                val remainingDaysToExam = maxOf(1, TimeUnit.MILLISECONDS.toDays(sem.examDate - System.currentTimeMillis()).toInt())
                SemanticRecommendationEngine.computeSemanticRecommendations(
                    topics = allTopics,
                    dueList = due,
                    weakList = weak,
                    examDays = remainingDaysToExam
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Monitor semesters and select the latest compiled one automatically if none is selected
        viewModelScope.launch {
            semesters.collect { list ->
                if (_selectedSemester.value == null && list.isNotEmpty()) {
                    selectSemester(list.first())
                }
            }
        }
    }

    fun navigateTo(screen: StudyScreen) {
        _currentScreen.value = screen
    }

    fun selectSemester(semester: Semester) {
        _selectedSemester.value = semester
        _selectedSubject.value = null
        _selectedTopic.value = null
        _teachingContent.value = null
        _activeQuiz.value = null
        _condensedPrepContent.value = null
        _activeSection.value = SemesterWorkspaceSection.Overview
        calculateStudyNowRecommendation(semester)
    }

    fun selectSection(section: SemesterWorkspaceSection) {
        _activeSection.value = section
    }

    fun selectSubject(subject: Subject) {
        _selectedSubject.value = subject
        _selectedTopic.value = null
        _teachingContent.value = null
        _activeQuiz.value = null
    }

    fun selectExplanationMode(mode: String) {
        _selectedExplanationMode.value = mode
        _selectedTopic.value?.let { topic ->
            _selectedSubject.value?.let { subject ->
                teachTopic(topic, subject, mode)
            }
        }
    }

    fun selectTopic(topic: Topic) {
        _selectedTopic.value = topic
        _teachingContent.value = null
        _activeQuiz.value = null
        _followUps.value = emptyList() // Clear for new topic context
        _currentScreen.value = StudyScreen.TutorWorkspace
        _selectedSubject.value?.let { subject ->
            teachTopic(topic, subject, _selectedExplanationMode.value)
        }
    }

    fun toggleTopicConfusing(topic: Topic) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = topic.copy(
                isConfusing = !topic.isConfusing,
                weakScore = if (!topic.isConfusing) 0.85f else 0.35f
            )
            repository.updateTopic(updated)
            if (_selectedTopic.value?.id == topic.id) {
                withContext(Dispatchers.Main) {
                    _selectedTopic.value = updated
                }
            }
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

    fun toggleTopicNeedsRevision(topic: Topic) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = topic.copy(
                needsRevision = !topic.needsRevision,
                revisionFrequency = if (!topic.needsRevision) topic.revisionFrequency + 1 else topic.revisionFrequency
            )
            repository.updateTopic(updated)
            if (_selectedTopic.value?.id == topic.id) {
                withContext(Dispatchers.Main) {
                    _selectedTopic.value = updated
                }
            }
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

    fun askFollowUpQuestion(topic: Topic, subjectName: String, query: String) {
        if (query.isBlank()) return
        _isFollowUpLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentText = _teachingContent.value ?: "No context available."
                val response = if (GeminiClient.isApiKeyAvailable) {
                    val systemPrompt = "You are Study Echo, answering a concise, high-academic value follow-up inquiry on ${topic.name} in the course $subjectName."
                    val finalPrompt = "Context explanation of the topic:\n$currentText\n\nStudent's follow-up query: $query\nProvide a brilliant, complete academic answer including formulas or structured steps."
                    GeminiClient.executePrompt(finalPrompt, systemPrompt)
                } else {
                    GeminiClient.answerFollowUp(topic.name, subjectName, query, currentText)
                }
                
                // Save exchange in academic memory store
                AcademicMemoryStore.logExchange(topic.id, query, response)
                
                withContext(Dispatchers.Main) {
                    val currentList = _followUps.value.toMutableList()
                    currentList.add(Pair(query, response))
                    _followUps.value = currentList
                }
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Error answering follow up: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isFollowUpLoading.value = false
                }
            }
        }
    }

    fun clearFollowUpsForTopic() {
        _followUps.value = emptyList()
    }

    // --- Create Custom Semester from Setup UI ---
    fun createSemester(
        name: String,
        startDate: Long,
        endDate: Long,
        examDate: Long,
        studyGoals: String,
        subjectNames: List<String>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val semId = repository.insertSemester(
                Semester(
                    name = name,
                    startDate = startDate,
                    endDate = endDate,
                    examDate = examDate,
                    studyGoals = studyGoals
                )
            ).toInt()

            val colors = listOf(
                0xFF3B82F6.toInt(), // Academic Blue
                0xFF10B981.toInt(), // Success Emerald
                0xFF8B5CF6.toInt(), // Mystic Violet
                0xFFEF4444.toInt(), // Target Crimson
                0xFFF59E0B.toInt()  // Amber Warm
            )

            // Split subjects and auto initialize difficulties
            subjectNames.forEachIndexed { index, subName ->
                if (subName.isNotBlank()) {
                    val fallbackColor = colors[index % colors.size]
                    val diff = when (index % 3) {
                        0 -> "Easy"
                        1 -> "Medium"
                        else -> "Hard"
                    }
                    val code = "SUB-${101 + index}"
                    val priority = when (index % 3) {
                        0 -> "High"
                        1 -> "Medium"
                        else -> "Low"
                    }
                    repository.insertSubject(
                        Subject(
                            semesterId = semId,
                            name = subName.trim(),
                            color = fallbackColor,
                            difficulty = diff,
                            subjectCode = code,
                            priority = priority,
                            completionProgress = 15 // Initial simulated progress
                        )
                    )
                }
            }

            // Immediately select the created workspace
            val createdSem = repository.getSemesterById(semId)
            if (createdSem != null) {
                withContext(Dispatchers.Main) {
                    selectSemester(createdSem)
                    _activeSection.value = SemesterWorkspaceSection.Overview
                    navigateTo(StudyScreen.Dashboard)
                }
            }
        }
    }

    // --- Create Unlimited Subject manually inside Semester ---
    fun createSubject(
        name: String,
        code: String,
        color: Int,
        difficulty: String,
        priority: String,
        completionProgress: Int
    ) {
        val sId = _selectedSemester.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val sub = Subject(
                semesterId = sId,
                name = name,
                color = color,
                difficulty = difficulty,
                subjectCode = code,
                priority = priority,
                completionProgress = completionProgress
            )
            repository.insertSubject(sub)
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

    // --- Update Syllabus completion progress or priority ---
    fun updateSubjectDetails(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSubject(subject)
            if (_selectedSubject.value?.id == subject.id) {
                withContext(Dispatchers.Main) {
                    _selectedSubject.value = subject
                }
            }
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

     // --- Drag-and-drop Material File Upload with simulated progress and automated metadata indexing ---
    fun uploadSubjectFile(
        subjectId: Int,
        fileName: String,
        fileType: String,
        fileSize: String,
        extractedSyllabusText: String? = null
    ) {
        viewModelScope.launch {
            _uploadProgress.value = 0.0f
            for (i in 1..5) {
                kotlinx.coroutines.delay(100)
                _uploadProgress.value = i * 0.20f
            }
            
            val simulatedFileText = extractedSyllabusText ?: """
                COLLEGE COURSE OUTLINE FOR SYLLABUS: APPLIED RESEARCH
                Unit I: Introductory Paradigms
                Chapter 1: Foundations of Axioms in $fileName
                Suggested study: 45 minutes. Core formulas: R_b = V / I. 
                Unit II: Applied Models
                Chapter 2: Synthesis and Neural Retraction
                Suggested study: 60 minutes. Prerequisite: Foundations of Axioms.
            """.trimIndent()
            
            _selectedSubject.value?.let { subject ->
                try {
                    val parsedStructure = DocumentIntelligencePipeline.analyzeDocumentStructure(
                        fileName = fileName,
                        fileType = fileType,
                        fileContentSimulatedText = simulatedFileText,
                        subjectName = subject.name
                    )
                    
                    val chaptersSummary = StringBuilder()
                    chaptersSummary.append("Title: ${parsedStructure.title}\n")
                    chaptersSummary.append("Objectives: ${parsedStructure.description}\n\n")
                    for (unit in parsedStructure.units) {
                        chaptersSummary.append("Unit: ${unit.name} (Complexity: ${unit.complexLevel})\n")
                        for (ch in unit.chapters) {
                            chaptersSummary.append("- ${ch.title} (Time: ${ch.suggestedStudyMinutes} min)\n")
                            if (ch.coreConcepts.isNotEmpty()) {
                                chaptersSummary.append("  Concepts: ${ch.coreConcepts.joinToString(", ")}\n")
                            }
                            if (ch.essentialFormulae.isNotEmpty()) {
                                chaptersSummary.append("  Formulae: ${ch.essentialFormulae.joinToString(", ")}\n")
                            }
                        }
                    }
                    if (parsedStructure.learningPathDependencies.isNotEmpty()) {
                        chaptersSummary.append("\nPrerequisites:\n")
                        for (dep in parsedStructure.learningPathDependencies) {
                            chaptersSummary.append("- ${dep.topicName} requires ${dep.requiresPrerequisiteTopic} (${dep.dependencyStrength})\n")
                        }
                    }
                    
                    val newFile = UploadedFile(
                        subjectId = subjectId,
                        name = fileName,
                        fileType = fileType,
                        fileSize = fileSize,
                        extractedChaptersText = chaptersSummary.toString()
                    )
                    
                    withContext(Dispatchers.IO) {
                        repository.insertUploadedFile(newFile)
                        
                        val dbTopics = mutableListOf<Topic>()
                        var orderIdx = 0
                        for (unit in parsedStructure.units) {
                            for (ch in unit.chapters) {
                                dbTopics.add(
                                    Topic(
                                        subjectId = subjectId,
                                        name = ch.title,
                                        orderIndex = orderIdx++,
                                        weakScore = 0.5f,
                                        nextRevisionDate = System.currentTimeMillis() + (orderIdx * 24 * 3600 * 1000L),
                                        importance = if (unit.complexLevel == "High") "High" else "Medium",
                                        estimatedStudyTimeMinutes = ch.suggestedStudyMinutes,
                                        summary = "Concepts: " + ch.coreConcepts.joinToString(", ") + ". Formulae: " + ch.essentialFormulae.joinToString(", ")
                                    )
                                )
                            }
                        }
                        if (dbTopics.isNotEmpty()) {
                            repository.insertTopics(dbTopics)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StudyViewModel", "Error parsing uploaded file: ${e.message}")
                    val newFile = UploadedFile(
                        subjectId = subjectId,
                        name = fileName,
                        fileType = fileType,
                        fileSize = fileSize,
                        extractedChaptersText = "Extracted summary for $fileName"
                    )
                    withContext(Dispatchers.IO) {
                        repository.insertUploadedFile(newFile)
                    }
                }
            } ?: run {
                val newFile = UploadedFile(
                    subjectId = subjectId,
                    name = fileName,
                    fileType = fileType,
                    fileSize = fileSize,
                    extractedChaptersText = "Extracted summary for $fileName"
                )
                withContext(Dispatchers.IO) {
                    repository.insertUploadedFile(newFile)
                }
            }
            
            _uploadProgress.value = null
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

    fun deleteUploadedFile(file: UploadedFile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUploadedFile(file)
        }
    }

    // --- Chapter and Topic (Roadmap Segment) Manual Insertion ---
    fun createManualTopic(
        subjectId: Int,
        name: String,
        importance: String,
        estimatedTimeMinutes: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val subTopics = repository.getTopicsForSubjectSync(subjectId)
            val nextIndex = (subTopics.maxOfOrNull { it.orderIndex } ?: -1) + 1
            val topic = Topic(
                subjectId = subjectId,
                name = name,
                orderIndex = nextIndex,
                importance = importance,
                estimatedStudyTimeMinutes = estimatedTimeMinutes,
                weakScore = 0.5f,
                nextRevisionDate = System.currentTimeMillis()
            )
            repository.insertTopic(topic)
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

    // --- Toggle topic completed directly on study sheets ---
    fun toggleTopicCompleted(topic: Topic) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = topic.copy(isCompleted = !topic.isCompleted)
            repository.updateTopic(updated)
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

    // --- Save active note-taking inside interactive study workspace ---
    fun saveTopicNotes(topicId: Int, notes: String) {
        val currentNotes = _topicNotes.value.toMutableMap()
        currentNotes[topicId] = notes
        _topicNotes.value = currentNotes
    }

    // --- Extract Syllabus roadmap using Gemini AI ---
    fun uploadCourseMaterials(subject: Subject, materialsText: String) {
        viewModelScope.launch {
            val updated = subject.copy(materialsText = materialsText)
            withContext(Dispatchers.IO) {
                repository.updateSubject(updated)
            }
            _selectedSubject.value = updated

            _isTeachingLoading.value = true
            try {
                // Generates linear topics using actual syllabus structure parsing
                val generated = withContext(Dispatchers.IO) {
                    GeminiClient.generateLearningRoadmap(subject.name, materialsText)
                }

                val dbTopics = generated.map { temp ->
                    Topic(
                        subjectId = subject.id,
                        name = temp.name,
                        orderIndex = temp.order,
                        weakScore = 0.5f,
                        nextRevisionDate = System.currentTimeMillis()
                    )
                }

                withContext(Dispatchers.IO) {
                    repository.insertTopics(dbTopics)
                }
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Error uploading syllabus: ${e.message}")
            } finally {
                _isTeachingLoading.value = false
            }
        }
    }

    // --- Patient Personal Tutoring System core logic ---
    fun setTeachingContentDirectly(content: String) {
        _teachingContent.value = content
    }

     private fun teachTopic(topic: Topic, subject: Subject, modeName: String) {
        _isTeachingLoading.value = true
        _teachingContent.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val examDays = _selectedSemester.value?.let { maxOf(1, TimeUnit.MILLISECONDS.toDays(it.examDate - now).toInt()) } ?: 30
                val allSubs = repository.getSubjectsForSemesterSync(subject.semesterId)
                val allTopics = allSubs.flatMap { repository.getTopicsForSubjectSync(it.id) }
                val due = allTopics.filter { it.nextRevisionDate <= now }
                val weak = allTopics.filter { it.weakScore > 0.6f }
                
                val memory = AcademicContextMemory(
                    activeSemester = _selectedSemester.value,
                    activeSubject = subject,
                    selectedTopic = topic,
                    targetExamDays = examDays,
                    weakSemesterTopicsCount = weak.size,
                    totalRevisionBacklogSize = due.size,
                    recentDoubtsAsked = emptyList(),
                    activeStudyMode = _currentStudyMode.value.name
                )
                
                val history = AcademicMemoryStore.getLogs(topic.id)
                
                val (systemInstruction, promptBody) = TeachingPromptOrchestrator.buildOrchestratedPrompt(
                    mode = modeName,
                    topicName = topic.name,
                    subjectName = subject.name,
                    memory = memory,
                    history = history
                )
                
                val explanation = if (GeminiClient.isApiKeyAvailable) {
                    GeminiClient.executePrompt(promptBody, systemInstruction)
                } else {
                    GeminiClient.teachConcept(topic.name, modeName, subject.name)
                }
                
                AcademicMemoryStore.logExchange(topic.id, "Explain Concept ($modeName)", explanation)
                
                withContext(Dispatchers.Main) {
                    _teachingContent.value = explanation
                    
                    val curHistory = _explanationHistory.value.toMutableList()
                    curHistory.add(Pair(topic.name, explanation))
                    _explanationHistory.value = curHistory

                    viewModelScope.launch(Dispatchers.IO) {
                        repository.recordStudySession(topic, rating = 2)
                        
                        val updatedStats = topic.copy(
                            studyCount = topic.studyCount + 1,
                            lastStudied = System.currentTimeMillis(),
                            studyTimeSpentSeconds = topic.studyTimeSpentSeconds + (topic.estimatedStudyTimeMinutes * 60L / 5)
                        )
                        repository.updateTopic(updatedStats)
                        if (_selectedTopic.value?.id == topic.id) {
                            withContext(Dispatchers.Main) {
                                _selectedTopic.value = updatedStats
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _teachingContent.value = "Error compiling personal guidance: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isTeachingLoading.value = false
                }
            }
        }
    }

    // --- Complete Active Recall Spaced Repetition Practice ---
    fun updateSpacedRepetition(topic: Topic, rating: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordStudySession(topic, rating)
            // Re-trigger study priority recommendation refresh
            _selectedSemester.value?.let { calculateStudyNowRecommendation(it) }
        }
    }

    // --- Generate and Play AI quiz tests ---
    fun loadInteractiveQuiz(topic: Topic, subjectName: String) {
        _isQuizLoading.value = true
        _activeQuiz.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val questions = GeminiClient.generateQuiz(topic.name, subjectName)
                withContext(Dispatchers.Main) {
                    _activeQuiz.value = questions
                }
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Quiz generation error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isQuizLoading.value = false
                }
            }
        }
    }

    // --- Flashcards generation and answering ---
    fun generateAIFlashcards(topic: Topic) {
        _isGeneratingFlashcards.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawCards = GeminiClient.generateFlashcards(topic.name)
                val dbCards = rawCards.map {
                    Flashcard(
                        topicId = topic.id,
                        question = it.question,
                        answer = it.answer
                    )
                }
                repository.insertFlashcards(dbCards)
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Flashcard generation error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isGeneratingFlashcards.value = false
                }
            }
        }
    }

    fun submitFlashcardReview(flashcard: Flashcard, score: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reviewFlashcard(flashcard, score)
        }
    }

    // --- Pre-Exam condensed revision sheets ---
    fun compileCondensedExamPrep(subjectName: String, topicsList: List<Topic>) {
        _isPrepLoading.value = true
        _condensedPrepContent.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val titles = topicsList.map { it.name }
                val sheet = GeminiClient.generateCondensedExamPrep(subjectName, titles)
                withContext(Dispatchers.Main) {
                    _condensedPrepContent.value = sheet
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _condensedPrepContent.value = "Failed creating pre-exam review: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isPrepLoading.value = false
                }
            }
        }
    }

    // --- "What Should I Study Now?" recommendation algorithm ---
    private fun calculateStudyNowRecommendation(semester: Semester) {
        viewModelScope.launch(Dispatchers.IO) {
            val allSubs = repository.getSubjectsForSemesterSync(semester.id)
            if (allSubs.isEmpty()) {
                _recommendation.value = null
                return@launch
            }

            var bestRecommendedTopic: Topic? = null
            var bestSub: Subject? = null
            var highestPriorityScore = -1f
            var reason = "No active subjects configured yet."

            val now = System.currentTimeMillis()
            val remainingDaysToExam = maxOf(1, TimeUnit.MILLISECONDS.toDays(semester.examDate - now).toInt())

            for (sub in allSubs) {
                val subTopics = repository.getTopicsForSubjectSync(sub.id)
                val subDiffMultiplier = when (sub.difficulty.lowercase()) {
                    "hard" -> 3.0f
                    "medium" -> 2.0f
                    else -> 1.0f
                }

                for (topic in subTopics) {
                    // Let's compute a study priority score for each specific topic
                    // Factors:
                    // 1. Completion status: Unfinished chapters (isCompleted=false) get premium point weight
                    val completionWeight = if (!topic.isCompleted) 50f else 10f

                    // 2a. High weakScore: student struggled previously (weakScore ranges 0.0 to 1.0)
                    val weaknessWeight = topic.weakScore * 40f

                    // 2b. Confusion indicator: student explicitly flagged as confusing
                    val confusingWeight = if (topic.isConfusing) 35f else 0f

                    // 2c. Need Revision indicator: student marked as needing active revision
                    val revisionRequirementWeight = if (topic.needsRevision) 25f else 0f

                    // 3. Spaced repetition overdueness: remaining time vs revision date
                    val overdueDeltaDays =  TimeUnit.MILLISECONDS.toDays(now - topic.nextRevisionDate).toFloat()
                    val urgencyWeight = (overdueDeltaDays.coerceAtLeast(0f) * 15f).coerceAtMost(50f)

                    // 4. Subject difficulty
                    val score = (completionWeight + weaknessWeight + confusingWeight + revisionRequirementWeight + urgencyWeight) * subDiffMultiplier

                    if (score > highestPriorityScore) {
                        highestPriorityScore = score
                        bestRecommendedTopic = topic
                        bestSub = sub
                        
                        reason = when {
                            remainingDaysToExam <= 14 -> "Urgent: Exam is in just $remainingDaysToExam days! This high-priority topic in ${sub.name} requires core revision."
                            !topic.isCompleted -> "New Concept: You haven't completed this chapter in ${sub.name} yet. Keep moving forward!"
                            topic.weakScore > 0.7f -> "Struggled Topic: Your recorded retention score on this is low. Practice active recall to reinforce memory."
                            else -> "Spaced Repetitive Cycle: Spaced repetition matches show it is due for recall reinforcement now."
                        }
                    }
                }
            }

            if (bestRecommendedTopic != null && bestSub != null) {
                _recommendation.value = TopicRecommendation(
                    topic = bestRecommendedTopic,
                    subjectName = bestSub.name,
                    reason = reason,
                    remainingDaysToExam = remainingDaysToExam
                )
            } else {
                _recommendation.value = null
            }
        }
    }
}

// Sealed routing screens matching design intents
sealed class StudyScreen {
    object Dashboard : StudyScreen()
    object SetupWorkspace : StudyScreen()
    object SubjectDetail : StudyScreen()
    object TutorWorkspace : StudyScreen()
    object QuizWorkspace : StudyScreen()
    object FlashcardWorkspace : StudyScreen()
    object PreExamPrep : StudyScreen()
    object RecallSession : StudyScreen()
}

data class TopicRecommendation(
    val topic: Topic,
    val subjectName: String,
    val reason: String,
    val remainingDaysToExam: Int
)
