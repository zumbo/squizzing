package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.domain.Round
import ch.quizzing.squizzing.repository.RoundRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RoundService(
    private val roundRepository: RoundRepository
) {

    fun findAll(): List<Round> = roundRepository.findAllOrderByStartDateDesc()

    fun findById(id: Long): Round? = roundRepository.findById(id).orElse(null)

    fun findActiveRound(): Round? = roundRepository.findByActiveTrue()

    @Transactional
    fun create(name: String, startDate: LocalDate, endDate: LocalDate): Round {
        val round = Round(
            name = name,
            startDate = startDate,
            endDate = endDate
        )
        return roundRepository.save(round)
    }

    @Transactional
    fun update(id: Long, name: String, startDate: LocalDate, endDate: LocalDate): Round? {
        val round = roundRepository.findById(id).orElse(null) ?: return null
        round.name = name
        round.startDate = startDate
        round.endDate = endDate
        return roundRepository.save(round)
    }

    @Transactional
    fun activate(id: Long): Round? {
        // Deactivate all other rounds
        roundRepository.findAll().forEach { round ->
            if (round.active) {
                round.active = false
                roundRepository.save(round)
            }
        }

        // Activate the specified round
        val round = roundRepository.findById(id).orElse(null) ?: return null
        round.active = true
        return roundRepository.save(round)
    }

    @Transactional
    fun deactivate(id: Long): Round? {
        val round = roundRepository.findById(id).orElse(null) ?: return null
        round.active = false
        return roundRepository.save(round)
    }

    @Transactional
    fun delete(id: Long) {
        roundRepository.deleteById(id)
    }
}
