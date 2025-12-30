package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.domain.PlayerRound
import ch.quizzing.squizzing.domain.Round
import ch.quizzing.squizzing.repository.PlayerRoundRepository
import ch.quizzing.squizzing.repository.RoundRepository
import org.springframework.stereotype.Service

data class ScoreboardEntry(
    val rank: Int,
    val displayName: String,
    val score: Int,
    val isCurrentUser: Boolean = false
)

@Service
class ScoreboardService(
    private val playerRoundRepository: PlayerRoundRepository,
    private val roundRepository: RoundRepository
) {

    fun getScoreboard(roundId: Long, currentUserId: Long? = null): List<ScoreboardEntry> {
        val playerRounds = playerRoundRepository.findCompletedByRoundIdOrderByScoreDesc(roundId)

        return playerRounds.mapIndexed { index, pr ->
            ScoreboardEntry(
                rank = index + 1,
                displayName = pr.user.displayName,
                score = pr.totalScore,
                isCurrentUser = pr.user.id == currentUserId
            )
        }
    }

    fun getAvailableRounds(): List<Round> {
        return roundRepository.findAllOrderByStartDateDesc()
    }

    fun findRound(id: Long): Round? {
        return roundRepository.findById(id).orElse(null)
    }

    fun getActiveRound(): Round? {
        return roundRepository.findByActiveTrue()
    }

    fun getUserRank(roundId: Long, userId: Long): Int? {
        val scoreboard = getScoreboard(roundId)
        return scoreboard.find { it.isCurrentUser }?.rank
    }
}
