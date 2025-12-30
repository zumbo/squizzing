package ch.quizzing.squizzing.repository

import ch.quizzing.squizzing.domain.MagicToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface MagicTokenRepository : JpaRepository<MagicToken, Long> {
    fun findByToken(token: String): MagicToken?

    @Modifying
    @Query("DELETE FROM MagicToken t WHERE t.expiresAt < :now OR t.used = true")
    fun deleteExpiredAndUsedTokens(now: Instant)
}
