package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val examDate: Long,
    val studyGoals: String = ""
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semesterId"])]
)
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semesterId: Int,
    val name: String,
    val color: Int, // Hex value representing the color
    val difficulty: String, // "Easy", "Medium", "Hard"
    val subjectCode: String = "",
    val priority: String = "Medium", // "High", "Medium", "Low"
    val completionProgress: Int = 0, // 0 - 100
    val materialsText: String? = null // Extracted syllabus, keywords, or contents
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val name: String,
    val orderIndex: Int,
    val isCompleted: Boolean = false,
    val weakScore: Float = 0.5f, // 0.0 means solid, 1.0 means extremely weak
    val lastStudied: Long = 0L,
    val revisionIntervalDays: Int = 1, // Spaced repetition interval
    val nextRevisionDate: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val rawText: String? = null, // Extracted text content or notes for this topic
    val importance: String = "Medium", // "Low", "Medium", "High"
    val estimatedStudyTimeMinutes: Int = 45,
    val isConfusing: Boolean = false,
    val needsRevision: Boolean = false,
    val studyTimeSpentSeconds: Long = 0L,
    val studyCount: Int = 0,
    val revisionFrequency: Int = 0
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["topicId"])]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Int,
    val question: String,
    val answer: String,
    val streak: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "uploaded_files",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class UploadedFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val name: String,
    val fileType: String, // "PDF", "Image", "Handwritten Notes", "Syllabus Screenshot"
    val uploadDate: Long = System.currentTimeMillis(),
    val fileSize: String = "0 B",
    val extractedChaptersText: String? = null
)
