package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- Semesters Queries ---
    @Query("SELECT * FROM semesters ORDER BY id DESC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester): Long

    @Update
    suspend fun updateSemester(semester: Semester)

    @Delete
    suspend fun deleteSemester(semester: Semester)

    @Query("SELECT * FROM semesters WHERE id = :id LIMIT 1")
    suspend fun getSemesterById(id: Int): Semester?


    // --- Subjects Queries ---
    @Query("SELECT * FROM subjects WHERE semesterId = :semesterId ORDER BY id ASC")
    fun getSubjectsForSemester(semesterId: Int): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE semesterId = :semesterId ORDER BY id ASC")
    suspend fun getSubjectsForSemesterSync(semesterId: Int): List<Subject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Int): Subject?


    // --- Topics Queries ---
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    fun getTopicsForSubject(subjectId: Int): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    suspend fun getTopicsForSubjectSync(subjectId: Int): List<Topic>

    @Query("SELECT * FROM topics ORDER BY lastStudied DESC LIMIT 20")
    fun getRecentTopics(): Flow<List<Topic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<Topic>)

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: Int): Topic?


    // --- Revision Queries (SM-2, weak topics, exam schedules) ---
    // Topics requiring revision across all subjects of an active semester
    @Query("""
        SELECT topics.* FROM topics 
        INNER JOIN subjects ON topics.subjectId = subjects.id 
        WHERE subjects.semesterId = :semesterId AND topics.nextRevisionDate <= :currentTime
        ORDER BY topics.weakScore DESC, topics.nextRevisionDate ASC
    """)
    fun getDueRevisionTopics(semesterId: Int, currentTime: Long): Flow<List<Topic>>

    // Select weakest topics across active semester
    @Query("""
        SELECT topics.* FROM topics 
        INNER JOIN subjects ON topics.subjectId = subjects.id 
        WHERE subjects.semesterId = :semesterId
        ORDER BY topics.weakScore DESC LIMIT :limit
    """)
    fun getWeakestTopics(semesterId: Int, limit: Int = 5): Flow<List<Topic>>


    // --- Flashcards Queries ---
    @Query("SELECT * FROM flashcards WHERE topicId = :topicId")
    fun getFlashcardsForTopic(topicId: Int): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE topicId = :topicId")
    suspend fun getFlashcardsForTopicSync(topicId: Int): List<Flashcard>

    @Query("""
        SELECT flashcards.* FROM flashcards 
        INNER JOIN topics ON flashcards.topicId = topics.id
        INNER JOIN subjects ON topics.subjectId = subjects.id
        WHERE subjects.semesterId = :semesterId AND flashcards.nextReviewDate <= :currentTime
        ORDER BY flashcards.nextReviewDate ASC
    """)
    fun getDueFlashcards(semesterId: Int, currentTime: Long): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>)

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)


    // --- Uploaded Files Queries ---
    @Query("SELECT * FROM uploaded_files WHERE subjectId = :subjectId ORDER BY uploadDate DESC")
    fun getUploadedFilesForSubject(subjectId: Int): Flow<List<UploadedFile>>

    @Query("""
        SELECT uploaded_files.* FROM uploaded_files 
        INNER JOIN subjects ON uploaded_files.subjectId = subjects.id 
        WHERE subjects.semesterId = :semesterId 
        ORDER BY uploaded_files.uploadDate DESC
    """)
    fun getAllUploadedFilesForSemester(semesterId: Int): Flow<List<UploadedFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadedFile(file: UploadedFile): Long

    @Delete
    suspend fun deleteUploadedFile(file: UploadedFile)
}
