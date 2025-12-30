package ch.quizzing.squizzing.repository

import ch.quizzing.squizzing.domain.PlayerAnswer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerAnswerRepository : JpaRepository<PlayerAnswer, Long> {
    fun findByPlayerRoundId(playerRoundId: Long): List<PlayerAnswer>
    fun countByPlayerRoundId(playerRoundId: Long): Int
    fun existsByPlayerRoundIdAndQuestionId(playerRoundId: Long, questionId: Long): Boolean
}
