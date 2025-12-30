package ch.quizzing.squizzing.repository

import ch.quizzing.squizzing.domain.AnswerOption
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnswerOptionRepository : JpaRepository<AnswerOption, Long> {
    fun findByQuestionIdOrderByOrderIndex(questionId: Long): List<AnswerOption>
}
