package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
        }
}

// Support definitions for generated structures
data class TempTopic(val name: String, val order: Int)
data class TempFlashcard(val question: String, val answer: String)
data class StudyQuizQuestion(val question: String, val options: List<String>, val correctIndex: Int, val explanation: String)

object GeminiClient {
    private const val TAG = "GeminiClient"

    // Retrieve API key. Securely check for placeholders
    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isEmpty() || key == "MY_GEMINI_API_KEY") "" else key
    }

    val isApiKeyAvailable: Boolean get() = getApiKey().isNotEmpty()

    suspend fun executePrompt(prompt: String, systemInstruction: String? = null): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            throw IllegalStateException("Gemini API key is not configured. Please add your key in the Secrets panel.")
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Received empty response from study model.")
    }

    /**
     * Parses syllabus text or outlines to generate structured topics
     */
    suspend fun generateLearningRoadmap(subjectName: String, materialsText: String): List<TempTopic> {
        if (!isApiKeyAvailable) {
            // Return rich educational mock content under normal demo expectations
            return listOf(
                TempTopic("Introduction to $subjectName & Core Principles", 1),
                TempTopic("Fundamental Concepts, Theories, and Frameworks", 2),
                TempTopic("Intermediate Analysis, Logic Modeling, and Equations", 3),
                TempTopic("Advanced Troubleshooting, Systems Engineering, and Real-World Application", 4),
                TempTopic("Exam Review: Synthesized Problems, Analytical Reviews, and Solutions", 5)
            )
        }

        val prompt = """
            You are an expert curriculum developer. Given the subject name "$subjectName" and raw materials or outline:
            "$materialsText"

            Generate a linear study roadmap of topics. Provide EXACTLY the topic titles in learning order, separated by a double newline.
            Do not write any introductory or concluding comments. Format it simply:
            Topic 1: [Name]
            Topic 2: [Name]
            ...
        """.trimIndent()

        return try {
            val response = executePrompt(prompt, "You are an intelligent syllabus and academic roadmap analyzer.")
            response.lines()
                .filter { it.isNotBlank() }
                .mapIndexed { index, line ->
                    val cleaned = line.replace(Regex("^Topic\\s+\\d+:\\s*"), "").trim()
                    TempTopic(cleaned, index + 1)
                }
                .filter { it.name.length > 3 }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating syllabus details: ${e.message}", e)
            listOf(
                TempTopic("Foundational Essentials of $subjectName", 1),
                TempTopic("Advanced Application Paradigms", 2),
                TempTopic("Exam Prep: Synthesizing Concepts", 3)
            )
        }
    }

    /**
     * Patient personal tutor mode teaching topics in study modes.
     */
    suspend fun teachConcept(topicName: String, mode: String, subjectName: String): String {
        if (!isApiKeyAvailable) {
            return """
                📚 [DEMO MODE - GEMINI KEY NOT SET]
                
                Here is a customized layout explanation of "$topicName" ($subjectName) in **$mode Mode**:
                
                ### Core Explanation
                $topicName represents one of the pivotal cornerstones of $subjectName. To grasp this fully, we look at the core structure:
                1. **Definition**: The fundamental baseline underlying the subject.
                2. **Application**: How this behaves under active circumstances.
                3. **Analogy**: Consider it like water repeating through a filtration grid. The water refines each cycle.
                
                ### Key Takeaway for Revision
                - Keep testing active recall.
                - Revisit this scheduled topic regularly to cement long-term comprehension.
                - Setup a quick flashcard quiz to evaluate retention!
                
                *(Configure your actual Gemini API Key in the AI Studio Secrets Panel to experience full generative AI mentoring.)*
            """.trimIndent()
        }

        val systemInstruction = """
            You are "Study Echo", an expert patient personal academic tutor. 
            Your goal is NOT to just chat, but to teach concepts with exquisite clarity, logical depth, beautifully organized headings, and step-by-step breakdowns.
            Adapt your response to the user's requested mode, and present information elegantly using Markdown.
        """.trimIndent()

        val prompt = when (mode.lowercase()) {
            "simple explanation" -> "Explain '$topicName' (from the subject '$subjectName') using beginner-friendly language, simplified steps, and simple everyday analogies."
            "detailed explanation" -> "Provide an in-depth, rigorous, comprehensive scholarly breakdown of '$topicName' ($subjectName) including specifications, mathematical formulations (if applicable), structural variables, and detailed processes."
            "exam-focused explanation" -> "Provide an exam-oriented study of '$topicName' ($subjectName). Highlight the typical questions examiners ask, critical scoring keywords, diagrams schemas, and bulleted concepts to memorize."
            "quick revision explanation" -> "Provide a dense, lightning-fast synthesis of '$topicName' ($subjectName) to read right before walking into an exam. Use high-concept summaries, tables, and short definitions."
            "real-world analogy explanation" -> "Explain '$topicName' ($subjectName) entirely using one extended, beautifully constructed, real-world metaphor or story that makes the abstract concepts concrete."
            else -> "Define and teach the academic concept '$topicName' ($subjectName) clearly and structure it for effective study."
        }

        return try {
            executePrompt(prompt, systemInstruction)
        } catch (e: Exception) {
            "Error rendering session: ${e.localizedMessage}. Please ensure your Gemini Key is verified."
        }
    }

    /**
     * Generate flashcards based on subject details or topic
     */
    suspend fun generateFlashcards(topicName: String, mode: String = "general"): List<TempFlashcard> {
        if (!isApiKeyAvailable) {
            return listOf(
                TempFlashcard("What is the primary objective of studying $topicName?", "To establish foundational mastery and link adjacent syllabus subjects."),
                TempFlashcard("What is a common real-world analogy associated with $topicName?", "An engine processing fuel cycle structures."),
                TempFlashcard("Name 3 core columns or properties that define $topicName.", "Execution speed, logical clarity, and modular structural boundaries.")
            )
        }

        val prompt = """
            You are an expert academic text synthesizer. Create a set of 4 study flashcards for the topic: "$topicName".
            Format each flashcard as exactly two lines:
            Q: [Question here]
            A: [Clear, concise study answer here]
            
            Repeat this matching pattern precisely. Do not include introductory text, numbers, or section markings.
        """.trimIndent()

        return try {
            val res = executePrompt(prompt, "You are an expert teacher generating study materials.")
            val cards = mutableListOf<TempFlashcard>()
            val lines = res.lines().filter { it.isNotBlank() }
            
            var i = 0
            while (i < lines.size - 1) {
                val qLine = lines[i]
                val aLine = lines[i + 1]
                if (qLine.startsWith("Q:") && aLine.startsWith("A:")) {
                    cards.add(TempFlashcard(qLine.substring(2).trim(), aLine.substring(2).trim()))
                    i += 2
                } else {
                    i++
                }
            }
            if (cards.isEmpty()) {
                throw IllegalStateException("Failed parser layout")
            }
            cards
        } catch (e: Exception) {
            listOf(
                TempFlashcard("Recall: Core Concept of $topicName", "Review the key details generated in your tutoring logs."),
                TempFlashcard("Application: Real-world scope of $topicName", "Used in solving critical operational parameters.")
            )
        }
    }

    /**
     * Generate a study quiz
     */
    suspend fun generateQuiz(topicName: String, subjectName: String): List<StudyQuizQuestion> {
        if (!isApiKeyAvailable) {
            return listOf(
                StudyQuizQuestion(
                    "Which statement best describes the fundamental purpose of $topicName?",
                    listOf("To increase performance overhead", "To organize complex syllabus concepts in clear schemas", "To minimize exam preparation speed"),
                    1,
                    "$topicName aims to organize syllabus structures nicely for retention."
                ),
                StudyQuizQuestion(
                    "What is a golden rule when revising critical themes in $subjectName?",
                    listOf("Cramming the night before", "Using Spaced Repetition and natural active recall systems", "Skipping complex modules entirely"),
                    1,
                    "Consistent active recall cycles yield optimal long-term memory."
                )
            )
        }

        val prompt = """
            You are an academic test engineer. Generate 3 high-quality multiple choice questions (with 3 options each) testing concept mastery of: "$topicName" (Subject: $subjectName).
            Format exactly like this for each question:
            Question: [The text]
            Option 1: [Option A]
            Option 2: [Option B]
            Option 3: [Option C]
            Correct Option: [Index number, either 1, 2, or 3]
            Explanation: [Short teaching explanation]
            
            Separate each complete question block by three dashes (---). Do not include formatting introductions.
        """.trimIndent()

        return try {
            val res = executePrompt(prompt, "You are a professional university test creator.")
            val questions = mutableListOf<StudyQuizQuestion>()
            val blocks = res.split("---")
            
            for (block in blocks) {
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                var qText = ""
                val options = mutableListOf<String>()
                var correctIdx = 0
                var explanation = ""
                
                for (line in lines) {
                    when {
                        line.startsWith("Question:") -> qText = line.substring(9).trim()
                        line.startsWith("Option 1:") -> options.add(line.substring(9).trim())
                        line.startsWith("Option 2:") -> options.add(line.substring(9).trim())
                        line.startsWith("Option 3:") -> options.add(line.substring(9).trim())
                        line.startsWith("Correct Option:") -> {
                            correctIdx = (line.substring(15).trim().toIntOrNull() ?: 1) - 1
                        }
                        line.startsWith("Explanation:") -> explanation = line.substring(12).trim()
                    }
                }
                
                if (qText.isNotEmpty() && options.size == 3) {
                    questions.add(StudyQuizQuestion(qText, options, correctIdx.coerceIn(0, 2), explanation))
                }
            }
            if (questions.isEmpty()) throw IllegalStateException("Empty parsed questions list")
            questions
        } catch (e: Exception) {
            listOf(
                StudyQuizQuestion(
                    "Quiz validation question for $topicName?",
                    listOf("Concept Option A", "Concept Option B", "Concept Option C"),
                    1,
                    "This test question validates connection reliability."
                )
            )
        }
    }

    /**
     * Last-minute formula/important questions condensed study sheet
     */
    suspend fun generateCondensedExamPrep(subjectName: String, topicsList: List<String>): String {
        if (!isApiKeyAvailable) {
            return """
                🔥 [DEMO MODE - GEMINI KEY NOT SET PREP SHEET]
                
                ## Last-Minute Study Guide: $subjectName
                
                ### 1. High-Probability Exam Prompts
                - Explain the governing framework unifying: ${topicsList.joinToString(", ")}.
                - Solve a hypothetical application scenario using the intermediate models.
                
                ### 2. High-Yield Golden Formulae & Syntheses
                - **Efficiency Factor**: $subjectName efficiency relates to active hours divided by distraction parameters.
                - **The Retention Rule**: Spacing is $subjectName learning intensity's primary catalyst.
                
                ### 3. Condensed Conceptual Reminders
                Ensure you remember:
                - Recall is twice as powerful as passing reading processes.
                - Review your generated flashcards immediately before exam entrance!
            """.trimIndent()
        }

        val prompt = """
            We are preparing for final exams in the subject '$subjectName'. 
            The curriculum includes:
            ${topicsList.joinToString("\n- ")}

            Generate an exquisite, high-intensity Condensed Last-Minute Revision Sheet.
            Provide:
            1. Table of "High-Probability Exam Prompts" with high-scoring keywords.
            2. A "Formula Study / Axiom Sheet" or logical rules summing up the subjects.
            3. A critical list of "Probable Important Pitfalls" to avoid in exam answers.
            
            Keep the content highly dense, focused on exam performance, and beautifully styled with clear headings.
        """.trimIndent()

        return try {
            executePrompt(prompt, "You are a top-tier professor specialized in exam prep condensation.")
        } catch (e: Exception) {
            "Error rendering Condensed Study Guide: ${e.localizedMessage}"
        }
    }

    /**
     * Answer follow-up curiosity or clarification questions about a studied topic
     */
    suspend fun answerFollowUp(topicName: String, subjectName: String, query: String, currentContext: String): String {
        if (!isApiKeyAvailable) {
            return """
                💡 [DEMO MODE - GEMINI KEY NOT SET]
                
                Regarding your question: "$query" on "$topicName", here is a focused academic perspective:
                - This concept deeply connects with the core variables of $subjectName.
                - When exploring "$query", remember that structural parameters must align with logical endpoints.
                
                Please configure your Gemini API key in the Secrets Panel to query the AI Coach regarding specific questions!
            """.trimIndent()
        }
        val systemInstruction = "You are 'Study Echo', a helpful, specialized academic mentor clarifying concept details."
        val prompt = """
            We are in an active study session for the topic '$topicName' under the subject '$subjectName'.
            This is the core tutoring document we are analyzing together:
            ---
            $currentContext
            ---

            The student has asked this specific follow-up question or clarification:
            "$query"

            Respond thoroughly and clearly. Structure your response with elegant bullet-points and markdown if useful, keeping it highly factual, educational, and direct.
        """.trimIndent()
        return try {
            executePrompt(prompt, systemInstruction)
        } catch (e: Exception) {
            "Study Echo was unable to parse follow-up details: ${e.localizedMessage}"
        }
    }
}
