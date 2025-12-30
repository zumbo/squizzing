package ch.quizzing.squizzing.repository

import ch.quizzing.squizzing.domain.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answerOptions WHERE q.round.id = :roundId ORDER BY q.orderIndex")
    fun findByRoundIdOrderByOrderIndex(roundId: Long): List<Question>

    fun countByRoundId(roundId: Long): Long
}
