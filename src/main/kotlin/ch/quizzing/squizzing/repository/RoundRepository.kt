package ch.quizzing.squizzing.repository

import ch.quizzing.squizzing.domain.Round
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoundRepository : JpaRepository<Round, Long> {
    fun findByActiveTrue(): Round?

    @Query("SELECT r FROM Round r ORDER BY r.startDate DESC")
    fun findAllOrderByStartDateDesc(): List<Round>
}
