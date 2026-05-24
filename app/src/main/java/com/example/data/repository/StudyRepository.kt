package com.example.data.repository

import com.example.data.db.StudyDao
import com.example.data.db.Semester
import com.example.data.db.Subject
import com.example.data.db.Topic
import com.example.data.db.Flashcard
import com.example.data.db.UploadedFile
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class StudyRepository(private val studyDao: StudyDao) {

    // --- Semesters ---
    val allSemesters: Flow<List<Semester>> = studyDao.getAllSemesters()

    suspend fun insertSemester(semester: Semester): Long = studyDao.insertSemester(semester)
    suspend fun updateSemester(semester: Semester) = studyDao.updateSemester(semester)
    suspend fun deleteSemester(semester: Semester) = studyDao.deleteSemester(semester)
    suspend fun getSemesterById(id: Int): Semester? = studyDao.getSemesterById(id)

    // --- Subjects ---
    fun getSubjectsForSemester(semesterId: Int): Flow<List<Subject>> = studyDao.getSubjectsForSemester(semesterId)
    suspend fun getSubjectsForSemesterSync(semesterId: Int): List<Subject> = studyDao.getSubjectsForSemesterSync(semesterId)
    suspend fun insertSubject(subject: Subject): Long = studyDao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = studyDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) = studyDao.deleteSubject(subject)
    suspend fun getSubjectById(id: Int): Subject? = studyDao.getSubjectById(id)

    // --- Topics ---
    fun getTopicsForSubject(subjectId: Int): Flow<List<Topic>> = studyDao.getTopicsForSubject(subjectId)
    suspend fun getTopicsForSubjectSync(subjectId: Int): List<Topic> = studyDao.getTopicsForSubjectSync(subjectId)
    val recentTopics: Flow<List<Topic>> = studyDao.getRecentTopics()
    suspend fun insertTopic(topic: Topic): Long = studyDao.insertTopic(topic)
    suspend fun insertTopics(topics: List<Topic>) = studyDao.insertTopics(topics)
    suspend fun updateTopic(topic: Topic) = studyDao.updateTopic(topic)
    suspend fun deleteTopic(topic: Topic) = studyDao.deleteTopic(topic)
    suspend fun getTopicById(id: Int): Topic? = studyDao.getTopicById(id)

    // --- Analytics, Scheduling, SM-2 Repetition Engine ---
    fun getDueRevisionTopics(semesterId: Int): Flow<List<Topic>> {
        return studyDao.getDueRevisionTopics(semesterId, System.currentTimeMillis())
    }

    fun getWeakestTopics(semesterId: Int, limit: Int = 5): Flow<List<Topic>> {
        return studyDao.getWeakestTopics(semesterId, limit)
    }

    // --- Flashcards ---
    fun getFlashcardsForTopic(topicId: Int): Flow<List<Flashcard>> = studyDao.getFlashcardsForTopic(topicId)
    suspend fun getFlashcardsForTopicSync(topicId: Int): List<Flashcard> = studyDao.getFlashcardsForTopicSync(topicId)
    fun getDueFlashcards(semesterId: Int): Flow<List<Flashcard>> {
        return studyDao.getDueFlashcards(semesterId, System.currentTimeMillis())
    }
    suspend fun insertFlashcard(flashcard: Flashcard): Long = studyDao.insertFlashcard(flashcard)
    suspend fun insertFlashcards(flashcards: List<Flashcard>) = studyDao.insertFlashcards(flashcards)
    suspend fun updateFlashcard(flashcard: Flashcard) = studyDao.updateFlashcard(flashcard)
    suspend fun deleteFlashcard(flashcard: Flashcard) = studyDao.deleteFlashcard(flashcard)

    /**
     * Spaced repetition update using an adaptation of SuperMemo SM-2 algorithm.
     * rating: 1 (forgot), 2 (hard / struggled), 3 (good / correct recall)
     */
    suspend fun recordStudySession(topic: Topic, rating: Int) {
        val lastInterval = topic.revisionIntervalDays
        val (newInterval, ratingDelta) = when (rating) {
            1 -> Pair(1, 0.2f) // Forgot - reset review to next day, highly weak
            2 -> Pair(maxOf(2, (lastInterval * 1.3).toInt()), 0.05f) // Struggled - slowly expand interval, medium weak
            3 -> Pair(maxOf(4, (lastInterval * 1.8).toInt()), -0.15f) // Great - expand interval aggressively, decrease weak score
            else -> Pair(1, 0.0f)
        }

        val nextDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, newInterval)
        }.timeInMillis

        // Update weak score within bounds of 0.0 to 1.0
        val targetWeakScore = (topic.weakScore + ratingDelta).coerceIn(0.0f, 1.0f)

        val updatedTopic = topic.copy(
            isCompleted = true,
            lastStudied = System.currentTimeMillis(),
            revisionIntervalDays = newInterval,
            nextRevisionDate = nextDate,
            weakScore = targetWeakScore
        )
        studyDao.updateTopic(updatedTopic)
    }

    /**
     * Flashcard reviews tracking
     * score: 1 (Wrong), 2 (Hard), 3 (Easy)
     */
    suspend fun reviewFlashcard(flashcard: Flashcard, score: Int) {
        val nextInDays = when (score) {
            1 -> 1
            2 -> maxOf(2, flashcard.streak + 1)
            3 -> maxOf(4, flashcard.streak * 2 + 1)
            else -> 1
        }
        val nextReview = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, nextInDays)
        }.timeInMillis

        val updated = flashcard.copy(
            streak = if (score == 1) 0 else flashcard.streak + 1,
            nextReviewDate = nextReview
        )
        studyDao.updateFlashcard(updated)
    }

    // --- Uploaded Files ---
    fun getUploadedFilesForSubject(subjectId: Int): Flow<List<UploadedFile>> = studyDao.getUploadedFilesForSubject(subjectId)
    fun getAllUploadedFilesForSemester(semesterId: Int): Flow<List<UploadedFile>> = studyDao.getAllUploadedFilesForSemester(semesterId)
    suspend fun insertUploadedFile(file: UploadedFile): Long = studyDao.insertUploadedFile(file)
    suspend fun deleteUploadedFile(file: UploadedFile) = studyDao.deleteUploadedFile(file)
}
