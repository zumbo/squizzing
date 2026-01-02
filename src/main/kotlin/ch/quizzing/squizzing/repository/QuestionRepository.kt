package ch.quizzing.squizzing.repository

import ch.quizzing.squizzing.domain.Question
import ch.quizzing.squizzing.domain.UserLanguage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answerOptions WHERE q.round.id = :roundId ORDER BY q.orderIndex, q.language")
    fun findByRoundIdOrderByOrderIndex(roundId: Long): List<Question>

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answerOptions WHERE q.round.id = :roundId AND q.language = :language ORDER BY q.orderIndex")
    fun findByRoundIdAndLanguageOrderByOrderIndex(roundId: Long, language: UserLanguage): List<Question>

    fun countByRoundId(roundId: Long): Long

    fun countByRoundIdAndLanguage(roundId: Long, language: UserLanguage): Long
}
