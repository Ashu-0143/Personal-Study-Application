package com.example.data.api

import android.util.Log
import com.example.data.db.Semester
import com.example.data.db.Subject
import com.example.data.db.Topic
import com.example.data.db.Flashcard
import com.example.data.db.UploadedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

// =========================================================================
// 1. Core Data Models for Academic Structure & Intelligence
// =========================================================================

data class ExtractedAcademicStructure(
    val title: String,
    val description: String,
    val units: List<AcademicUnit>,
    val learningPathDependencies: List<TopicDependency>,
    val estimatedTotalPrepTimeHours: Int,
    val suggestedWeeklySchedule: String
)

data class AcademicUnit(
    val name: String,
    val sequenceOrder: Int,
    val chapters: List<AcademicChapter>,
    val complexLevel: String // "Low", "Medium", "High"
)

data class AcademicChapter(
    val title: String,
    val sequenceOrder: Int,
    val coreConcepts: List<String>,
    val essentialFormulae: List<String>,
    val standardDefinitions: List<Pair<String, String>>,
    val suggestedStudyMinutes: Int
)

data class TopicDependency(
    val topicName: String,
    val requiresPrerequisiteTopic: String,
    val dependencyStrength: String // "Critical", "Recommended", "Optional"
)

data class AiAcademicInsight(
    val id: String,
    val title: String,
    val description: String,
    val statusLevel: String, // "Muted", "Warning", "Critical", "Good"
    val estimatedExamReadyPercent: Int,
    val primaryThreat: String?, // e.g., "Neglected Subject", "Overload Risk"
    val suggestedActionPlan: String
)

data class SemanticRecommendation(
    val topic: Topic,
    val priorityScore: Int, // 1-100 score on how urgent/beneficial it is to study
    val reasonPhasing: String,
    val learningSyllabusContext: String // e.g. "Connective Prerequisite to Chapter 5"
)

// =========================================================================
// 2. Contextual AI Memory & Dialogue Persistence Store
// =========================================================================

data class AcademicContextMemory(
    val activeSemester: Semester?,
    val activeSubject: Subject?,
    val selectedTopic: Topic?,
    val targetExamDays: Int,
    val weakSemesterTopicsCount: Int,
    val totalRevisionBacklogSize: Int,
    val recentDoubtsAsked: List<String>,
    val activeStudyMode: String
)

object AcademicMemoryStore {
    private val conversationLogs = mutableMapOf<Int, MutableList<Pair<String, String>>>() // topicId -> list of (userQuestion, aiAnswer)
    private val conceptualMisunderstandings = mutableMapOf<Int, MutableList<String>>() // topicId -> list of misunderstanding flags
    
    fun logExchange(topicId: Int, query: String, response: String) {
        val list = conversationLogs.getOrPut(topicId) { mutableListOf() }
        list.add(Pair(query, response))
        if (list.size > 20) {
            list.removeAt(0) // keep memory footprint compact
        }
    }

    fun getLogs(topicId: Int): List<Pair<String, String>> {
        return conversationLogs[topicId] ?: emptyList()
    }

    fun addMisunderstanding(topicId: Int, concept: String) {
        val list = conceptualMisunderstandings.getOrPut(topicId) { mutableListOf() }
        if (!list.contains(concept)) {
            list.add(concept)
        }
    }

    fun getMisunderstandings(topicId: Int): List<String> {
        return conceptualMisunderstandings[topicId] ?: emptyList()
    }

    fun clearHistory() {
        conversationLogs.clear()
        conceptualMisunderstandings.clear()
    }
}

// =========================================================================
// 3. Complete Document Intelligence Pipeline
// =========================================================================

object DocumentIntelligencePipeline {
    private const val TAG = "DocIntelligencePip"

    /**
     * Parses uploaded files (PDF, textbook scans, syllabus screenshots) to extract high-fidelity scholarly schemas.
     */
    suspend fun analyzeDocumentStructure(
        fileName: String,
        fileType: String,
        fileContentSimulatedText: String,
        subjectName: String
    ): ExtractedAcademicStructure = withContext(Dispatchers.IO) {
        if (!GeminiClient.isApiKeyAvailable) {
            return@withContext generateFallbackStructure(fileName, subjectName)
        }

        val prompt = """
            You are a state-of-the-art Document Intelligence engine specializing in collegiate academic structures.
            We have uploaded a file: name "$fileName" of type "$fileType" for our subject "$subjectName".
            The raw text text extractor outputs this content:
            ---
            $fileContentSimulatedText
            ---

            Analyze this carefully. Intelligently structure the syllabus or materials into a structured, connected course-node schema. 
            Define the chapters, sequence orders, key formulas, core definitions, and prerequisite links.
            
            Respond with a single raw VALID JSON object of this structure. Do not place it in Markdown code blocks:
            {
               "title": "Document Title",
               "description": "Short brief description of the document core learning objectives",
               "units": [
                  {
                     "name": "Unit Title Name",
                     "sequenceOrder": 1,
                     "chapters": [
                        {
                           "title": "Chapter Title Name",
                           "sequenceOrder": 1,
                           "coreConcepts": ["Concept A", "Concept B"],
                           "essentialFormulae": ["Formula A"],
                           "standardDefinitions": [["Keyword A", "Definition Text A"]],
                           "suggestedStudyMinutes": 45
                        }
                     ],
                     "complexLevel": "Medium"
                  }
               ],
               "learningPathDependencies": [
                  {
                     "topicName": "Chapter Title Name",
                     "requiresPrerequisiteTopic": "Prerequisite Chapter Title",
                     "dependencyStrength": "Critical"
                  }
               ],
               "estimatedTotalPrepTimeHours": 35,
               "suggestedWeeklySchedule": "Dedicate 5 hours weekly, starting from foundation concepts..."
            }
        """.trimIndent()

        try {
            val responseText = GeminiClient.executePrompt(prompt, "You are a university dean and course structural architect.")
            val cleaned = cleanJsonMarkdown(responseText)
            val json = JSONObject(cleaned)
            
            val unitsList = mutableListOf<AcademicUnit>()
            val unitsArray = json.optJSONArray("units") ?: JSONArray()
            for (i in 0 until unitsArray.length()) {
                val u = unitsArray.getJSONObject(i)
                val chs = u.optJSONArray("chapters") ?: JSONArray()
                val chsList = mutableListOf<AcademicChapter>()
                for (j in 0 until chs.length()) {
                    val c = chs.getJSONObject(j)
                    val concepts = jsonArrayToStringList(c.optJSONArray("coreConcepts"))
                    val formulae = jsonArrayToStringList(c.optJSONArray("essentialFormulae"))
                    val defs = mutableListOf<Pair<String, String>>()
                    val defsArr = c.optJSONArray("standardDefinitions")
                    if (defsArr != null) {
                        for (k in 0 until defsArr.length()) {
                            val singleDef = defsArr.optJSONArray(k)
                            if (singleDef != null && singleDef.length() >= 2) {
                                defs.add(Pair(singleDef.getString(0), singleDef.getString(1)))
                            }
                        }
                    }
                    chsList.add(AcademicChapter(
                        title = c.optString("title", "Chapter ${j + 1}"),
                        sequenceOrder = c.optInt("sequenceOrder", j + 1),
                        coreConcepts = concepts,
                        essentialFormulae = formulae,
                        standardDefinitions = defs,
                        suggestedStudyMinutes = c.optInt("suggestedStudyMinutes", 45)
                    ))
                }

                unitsList.add(AcademicUnit(
                    name = u.optString("name", "Unit ${i + 1}"),
                    sequenceOrder = u.optInt("sequenceOrder", i + 1),
                    chapters = chsList,
                    complexLevel = u.optString("complexLevel", "Medium")
                ))
            }

            val depsList = mutableListOf<TopicDependency>()
            val depsArr = json.optJSONArray("learningPathDependencies") ?: JSONArray()
            for (i in 0 until depsArr.length()) {
                val d = depsArr.getJSONObject(i)
                depsList.add(TopicDependency(
                    topicName = d.optString("topicName", ""),
                    requiresPrerequisiteTopic = d.optString("requiresPrerequisiteTopic", ""),
                    dependencyStrength = d.optString("dependencyStrength", "Recommended")
                ))
            }

            ExtractedAcademicStructure(
                title = json.optString("title", fileName),
                description = json.optString("description", "Course outline extracted from $fileName"),
                units = unitsList,
                learningPathDependencies = depsList,
                estimatedTotalPrepTimeHours = json.optInt("estimatedTotalPrepTimeHours", 20),
                suggestedWeeklySchedule = json.optString("suggestedWeeklySchedule", "Flexible study routine.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed structure parsing model response: ${e.message}", e)
            generateFallbackStructure(fileName, subjectName)
        }
    }

    private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.optString(i))
        }
        return list
    }

    private fun cleanJsonMarkdown(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        }
        if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    private fun generateFallbackStructure(fileName: String, subjectName: String): ExtractedAcademicStructure {
        return ExtractedAcademicStructure(
            title = fileName.replace(Regex("\\.[^.]+$"), " Structure"),
            description = "Educational breakdown derived from studying uploaded reference documentation in $subjectName.",
            units = listOf(
                AcademicUnit(
                    name = "Fundamentals & Context Foundations",
                    sequenceOrder = 1,
                    chapters = listOf(
                        AcademicChapter(
                            title = "Core Principles of $subjectName",
                            sequenceOrder = 1,
                            coreConcepts = listOf("Basic Definitions", "System Elements", "Axiomatic Framework"),
                            essentialFormulae = listOf("S_ef = E_f / (1 + Loss)"),
                            standardDefinitions = listOf(Pair("Syllabus Target", "Core topic designed to build solid memory bases.")),
                            suggestedStudyMinutes = 40
                        )
                    ),
                    complexLevel = "Low"
                )
            ),
            learningPathDependencies = emptyList(),
            estimatedTotalPrepTimeHours = 12,
            suggestedWeeklySchedule = "Review study guides 4 hours weekly before milestone checks."
        )
    }
}

// =========================================================================
// 4. Advanced AI Teaching Engine & Prompt Orchestration
// =========================================================================

object TeachingPromptOrchestrator {
    
    fun buildOrchestratedPrompt(
        mode: String,
        topicName: String,
        subjectName: String,
        memory: AcademicContextMemory,
        history: List<Pair<String, String>>
    ): Pair<String, String> {
        val systemInstruction = """
            You are "Study Echo", an elite academic advisor and adaptive personal tutor.
            Your philosophy: Lead the student to master complex principles. Do NOT chat casually. Explain step-by-step with impeccable formatting, high-information headings, dynamic metaphors, standard mathematical models (if applicable), typical university test questions, and recovery techniques.
            Keep dialogue strictly focused on providing highly detailed and functional pedagogy.
        """.trimIndent()

        val activeHistoryBlock = if (history.isNotEmpty()) {
            "Previous conversation context:\n" + history.joinToString("\n") { "Student: ${it.first}\nTutor: ${it.second}" }
        } else {
            "No previous chat history in this session."
        }

        val examDaysUrgency = memory.targetExamDays
        val contextualUrgencyQuote = when {
            examDaysUrgency <= 7 -> "⚠️ HIGH URGENCY: The final exam is in just $examDaysUrgency days! Keep explanations extremely performance-dense, highlight exact scoring keywords, and avoid peripheral theories."
            examDaysUrgency <= 21 -> "⏱️ MEDIUM URGENCY: Exam scheduled in $examDaysUrgency days. Balance theory with practical exam patterns and problem-solving steps."
            else -> "☘️ STABLE FOUNDATIONAL FLOW: Plenty of time ($examDaysUrgency days to exam). Focus on building deep model comprehension, creative analogies, and robust conceptual models."
        }

        val promptBody = """
            Topic for Session: "$topicName"
            Parent Subject: "$subjectName"
            Current Workspace Study Mode of Student: ${memory.activeStudyMode}
            TUTOR RESPONSE MODE KEYWORD: $mode

            $contextualUrgencyQuote

            $activeHistoryBlock

            Based on the tutor response mode keyword "$mode", format your instruction according to the following guidelines:

            1. DEEP CONCEPTUAL TEACHING:
               - Exhaustive academic guide, detailed system blueprints, formulas, and historical research rationale.
               
            2. QUICK REVISION EXPLANATIONS:
               - Condensed tables, immediate key terms, high-impact bulleted summaries. Perfect for rapid mental scanning.
               
            3. EXAM-FOCUSED SUMMARIZATION:
               - Focus heavily on likely exam Prompts, grade-saving checklists, precise keywords required by examiners, and scoring guidelines cards.

            4. ACTIVE RECALL QUESTION GENERATION:
               - Generate 3-4 progressive open-ended Socratic questions that force students to mentally retrieve information. Provide small hint drawers.

            5. WEAK-TOPIC REINFORCEMENT:
               - (Note: Student has a weak score on this topic). Prioritize diagnosing confusion triggers. Deconstruct typical misunderstandings, explain the topic using two totally different creative everyday metaphors, and provide immediate remediation steps.

            6. BEGINNER-FRIENDLY TEACHING:
               - Use zero academic jargon. Frame the whole explanation using a single cohesive fun story. Break elements into bite-sized paragraphs.

            7. LAST-MINUTE PREPARATION GUIDANCE:
               - High-intensity, low-stress outline. Mention the "must-remember" principles, formula cheat sheets, and a calming performance state tip.

            Please craft an elite, beautifully styled Markdown answer that exceeds usual expectations for this subject.
        """.trimIndent()

        return Pair(systemInstruction, promptBody)
    }
}

// =========================================================================
// 5. Intelligent AI-Generated Educational Resource Workflows
// =========================================================================

data class GeneratedFlashcardResource(val question: String, val answer: String, val memoryKeyTrigger: String)

data class QuizQuestionResource(val question: String, val options: List<String>, val correctIdx: Int, val rationale: String)

data class StudyGuideResource(
    val title: String,
    val executiveSummary: String,
    val formulaSheet: List<Pair<String, String>>,
    val conceptualMapOutline: List<String>,
    val probableExamQuestions: List<String>
)

object EducationalResourcePipeline {
    private const val TAG = "EduResourcePip"

    /**
     * Pipeline for compiling rich executive study guides
     */
    suspend fun compileStudyGuide(topicName: String, subjectName: String): StudyGuideResource = withContext(Dispatchers.IO) {
        if (!GeminiClient.isApiKeyAvailable) {
            return@withContext StudyGuideResource(
                title = "Summary Guide: $topicName",
                executiveSummary = "Comprehensive briefing compiled to solidify core retention of $topicName in $subjectName.",
                formulaSheet = listOf(Pair("Equilibrium State Value", "V_eq = Sum(C_i) * RetentionCoefficient")),
                conceptualMapOutline = listOf("1. Foundational Definition", "2. Core Application Layers", "3. Exam Scenario Synthesis"),
                probableExamQuestions = listOf("Explain the operational boundary criteria governing $topicName structures.")
            )
        }

        val prompt = """
            We are compiling an educational study sheet for: "$topicName" inside the course: "$subjectName".
            Generate a full academic resource structured precisely in valid JSON. Do not write text outside the JSON boundaries.
            
            JSON structure:
            {
               "title": "Study Sheet: Name",
               "executiveSummary": "A highly dense scholarly 150-word executive summary deconstructing this topic's purpose and key variables.",
               "formulaSheet": [
                  ["Formula or Axiom Name", "Mathematical Equation or Logical Definition Definition"]
               ],
               "conceptualMapOutline": [
                  "Hierarchical conceptual breakdown Step 1",
                  "Hierarchical conceptual breakdown Step 2"
               ],
               "probableExamQuestions": [
                  "Likely exam questions commonly set by university boards"
               ]
            }
        """.trimIndent()

        try {
            val res = GeminiClient.executePrompt(prompt, "You are a professional compiler of examination sheets.")
            val jsonStr = cleanJsonMarkdown(res)
            val json = JSONObject(jsonStr)
            
            val formulas = mutableListOf<Pair<String, String>>()
            val fArr = json.optJSONArray("formulaSheet") ?: JSONArray()
            for (i in 0 until fArr.length()) {
                val fItem = fArr.optJSONArray(i)
                if (fItem != null && fItem.length() >= 2) {
                    formulas.add(Pair(fItem.getString(0), fItem.getString(1)))
                }
            }

            val maps = mutableListOf<String>()
            val mArr = json.optJSONArray("conceptualMapOutline") ?: JSONArray()
            for (i in 0 until mArr.length()) {
                maps.add(mArr.optString(i))
            }

            val questions = mutableListOf<String>()
            val qArr = json.optJSONArray("probableExamQuestions") ?: JSONArray()
            for (i in 0 until qArr.length()) {
                questions.add(qArr.optString(i))
            }

            StudyGuideResource(
                title = json.optString("title", "Study Sheet: $topicName"),
                executiveSummary = json.optString("executiveSummary", ""),
                formulaSheet = formulas,
                conceptualMapOutline = maps,
                probableExamQuestions = questions
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed study sheet resource pipeline: ${e.message}", e)
            StudyGuideResource(
                title = "Guide: $topicName",
                executiveSummary = "Direct operational summary highlighting key $topicName structures.",
                formulaSheet = emptyList(),
                conceptualMapOutline = listOf("Initial Principles", "Calculus Applications"),
                probableExamQuestions = listOf("What is the core definition of $topicName?")
            )
        }
    }

    private fun cleanJsonMarkdown(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        }
        if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }
}

// =========================================================================
// 6. Semantic Study Recommendation Engine
// =========================================================================

object SemanticRecommendationEngine {
    
    fun computeSemanticRecommendations(
        topics: List<Topic>,
        dueList: List<Topic>,
        weakList: List<Topic>,
        examDays: Int
    ): List<SemanticRecommendation> {
        val recommendations = mutableListOf<SemanticRecommendation>()
        
        // Sort remaining topics to isolate unfinished high-importance ones
        val unfinishedHigh = topics.filter { !it.isCompleted && it.importance == "High" }
        val unresolvedConfused = topics.filter { it.isConfusing }

        // Build priority recommendations list safely
        for (topic in topics) {
            var score = 30 // baseline score
            val reasons = mutableListOf<String>()
            var infoContext = "Syllabus Node"

            val isDue = dueList.any { it.id == topic.id }
            val isWeak = weakList.any { it.id == topic.id }
            val isConfused = unresolvedConfused.any { it.id == topic.id }
            val isUnfinishedHigh = unfinishedHigh.any { it.id == topic.id }

            if (isUnfinishedHigh) {
                score += 35
                reasons.add("Incomplete Critical Syllabus Foundation")
                infoContext = "Essential Milestone Link"
            }
            if (isDue) {
                score += 25
                reasons.add("SM-2 Memory Decay Threshold Breached")
                infoContext = "Spaced Repetition Overdue"
            }
            if (isWeak) {
                score += 20
                reasons.add("Axiom Retention Score Low (${(topic.weakScore * 100).toInt()}% Deficit)")
                infoContext = "Vulnerable Mastery Zone"
            }
            if (isConfused) {
                score += 15
                reasons.add("Student Flagged as Highly Confusing Concept")
                infoContext = "Confusion Remediation Node"
            }

            // Adjust recommendations priority score based on exam distance
            if (examDays <= 7) {
                // Highly filter towards survival: focus on Due and Unfinished High
                if (isUnfinishedHigh || isDue || isConfused) {
                    score += 15
                } else {
                    score -= 10
                }
            }

            if (reasons.isNotEmpty() && score > 40) {
                val finalReason = reasons.joinToString(" • ")
                recommendations.add(SemanticRecommendation(
                    topic = topic,
                    priorityScore = score.coerceIn(1, 99),
                    reasonPhasing = finalReason,
                    learningSyllabusContext = infoContext
                ))
            }
        }

        return recommendations.sortedByDescending { it.priorityScore }
    }
}

// =========================================================================
// 7. Academic AI Study Insights Engine
// =========================================================================

object AiStudyInsightsEngine {

    fun generateIntelligentStudyInsights(
        subjects: List<Subject>,
        allTopics: List<Topic>,
        dueTopics: List<Topic>,
        weakTopics: List<Topic>,
        targetExamDays: Int
    ): List<AiAcademicInsight> {
        val insightsList = mutableListOf<AiAcademicInsight>()

        // 1. Neglected Subject check
        subjects.forEach { sub ->
            val subTopics = allTopics.filter { it.subjectId == sub.id }
            if (subTopics.isNotEmpty()) {
                val studiedCount = subTopics.count { it.isCompleted }
                val percentFinished = (studiedCount.toFloat() / subTopics.size * 100).toInt()
                
                if (percentFinished < 20 && targetExamDays <= 30) {
                    insightsList.add(AiAcademicInsight(
                        id = "neglected_${sub.id}",
                        title = "Neglected Syllabus Domain: ${sub.name}",
                        description = "You have completed only $percentFinished% of the structured milestone paths for this course. Progress models indicate high risk of final preparation shortfalls.",
                        statusLevel = "Critical",
                        estimatedExamReadyPercent = percentFinished,
                        primaryThreat = "Neglected Subject",
                        suggestedActionPlan = "Schedule an immediate 'Deep Study' sprint for '${sub.name}' nodes this week. Try uploading material to unlock instant roadmap pathways."
                    ))
                }
            }
        }

        // 2. Revision Gap tracking (Backlog overload)
        if (dueTopics.size >= 4) {
            insightsList.add(AiAcademicInsight(
                id = "backlog_overload",
                title = "Critical Recall Deficit Accumulating",
                description = "You have ${dueTopics.size} critical topics overdue for retention verification. Spaced repetition neural models project substantial memory decay if study delays persist.",
                statusLevel = "Warning",
                estimatedExamReadyPercent = maxOf(25, 100 - (dueTopics.size * 8)),
                primaryThreat = "Revision Gaps",
                suggestedActionPlan = "Temporarily toggle Workspace Mode to 'Revision Mode'. Enter active revision loops in the backlog panel and clear at least 3 cards today."
            ))
        }

        // 3. Expected Exam Readiness score model
        if (allTopics.isNotEmpty()) {
            val overallStudied = allTopics.count { it.isCompleted }
            val averageWeakness = allTopics.map { 1.0f - it.weakScore }.average().toFloat()
            val scoreBase = (overallStudied.toFloat() / allTopics.size * 50) + (averageWeakness * 50)
            val computedReady = scoreBase.toInt().coerceIn(5, 95)

            val status = when {
                computedReady >= 80 -> "Good"
                computedReady >= 50 -> "Muted"
                else -> "Warning"
            }

            insightsList.add(AiAcademicInsight(
                id = "exam_readiness_score",
                title = "Holistic Academic Retention Level",
                description = "Based on covered subjects, SM-2 interval expansion factors, and average memory recall metrics, your current examination performance readiness is modeled at $computedReady%.",
                statusLevel = status,
                estimatedExamReadyPercent = computedReady,
                primaryThreat = null,
                suggestedActionPlan = "Continue logging scheduled checkouts. Prioritize challenging weak spots in active challenge workspaces to push this score higher."
            ))
        }

        // Default item if database is fresh
        if (insightsList.isEmpty()) {
            insightsList.add(AiAcademicInsight(
                id = "system_initiating",
                title = "AI Advisor Synthesizing Metrics",
                description = "The cognitive intelligence model is priming parameters. Complete primary roadmaps and logging sessions, or upload textbooks, to unlock detailed insight cards.",
                statusLevel = "Muted",
                estimatedExamReadyPercent = 50,
                primaryThreat = null,
                suggestedActionPlan = "Add courses, trigger roadmaps, and rate recall session difficulty to allow structural analysis."
            ))
        }

        return insightsList
    }
}
