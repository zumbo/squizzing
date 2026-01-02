package ch.quizzing.squizzing.repository

import ch.quizzing.squizzing.domain.PlayerRound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PlayerRoundRepository : JpaRepository<PlayerRound, Long> {
    fun findByUserIdAndRoundId(userId: Long, roundId: Long): PlayerRound?

    @Query("SELECT pr FROM PlayerRound pr WHERE pr.round.id = :roundId AND pr.completedAt IS NOT NULL ORDER BY pr.totalScore DESC")
    fun findCompletedByRoundIdOrderByScoreDesc(roundId: Long): List<PlayerRound>

    @Query("SELECT pr FROM PlayerRound pr WHERE pr.user.id = :userId ORDER BY pr.startedAt DESC")
    fun findByUserIdOrderByStartedAtDesc(userId: Long): List<PlayerRound>

    fun existsByUserIdAndRoundId(userId: Long, roundId: Long): Boolean

    @Query("SELECT DISTINCT pr FROM PlayerRound pr LEFT JOIN FETCH pr.round LEFT JOIN FETCH pr.answers a LEFT JOIN FETCH a.question q LEFT JOIN FETCH q.answerOptions LEFT JOIN FETCH a.selectedAnswer WHERE pr.id = :id")
    fun findByIdWithAnswers(id: Long): PlayerRound?
}
