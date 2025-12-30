package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.domain.*
import ch.quizzing.squizzing.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

data class QuizState(
    val playerRound: PlayerRound,
    val currentQuestionIndex: Int,
    val totalQuestions: Int,
    val currentQuestion: Question?,
    val questionShownAt: Instant?,
    val isCompleted: Boolean
)

data class AnswerResult(
    val correct: Boolean,
    val score: Int,
    val correctAnswer: AnswerOption,
    val explanation: String?,
    val hasNextQuestion: Boolean
)

@Service
class QuizService(
    private val roundRepository: RoundRepository,
    private val questionRepository: QuestionRepository,
    private val answerOptionRepository: AnswerOptionRepository,
    private val playerRoundRepository: PlayerRoundRepository,
    private val playerAnswerRepository: PlayerAnswerRepository
) {

    companion object {
        const val TIMER_SECONDS = 10
        const val MAX_SCORE = 100
        const val MIN_SCORE = 50
    }

    fun canPlayRound(userId: Long, roundId: Long): Boolean {
        // Check if round exists and is active
        val round = roundRepository.findById(roundId).orElse(null) ?: return false
        if (!round.active) return false

        // Check if user has already completed the round
        val playerRound = playerRoundRepository.findByUserIdAndRoundId(userId, roundId)
        return playerRound == null || !playerRound.isCompleted()
    }

    fun hasCompletedRound(userId: Long, roundId: Long): Boolean {
        val playerRound = playerRoundRepository.findByUserIdAndRoundId(userId, roundId)
        return playerRound?.isCompleted() == true
    }

    @Transactional
    fun startOrResumeQuiz(user: User, roundId: Long): QuizState? {
        val round = roundRepository.findById(roundId).orElse(null) ?: return null

        // Find or create player round
        var playerRound = playerRoundRepository.findByUserIdAndRoundId(user.id, roundId)
        if (playerRound == null) {
            playerRound = PlayerRound(user = user, round = round)
            playerRound = playerRoundRepository.save(playerRound)
        }

        if (playerRound.isCompleted()) {
            return QuizState(
                playerRound = playerRound,
                currentQuestionIndex = 0,
                totalQuestions = 0,
                currentQuestion = null,
                questionShownAt = null,
                isCompleted = true
            )
        }

        val questions = questionRepository.findByRoundIdOrderByOrderIndex(roundId)
        val answeredCount = playerAnswerRepository.countByPlayerRoundId(playerRound.id)

        val currentQuestion = if (answeredCount < questions.size) {
            questions[answeredCount]
        } else {
            null
        }

        return QuizState(
            playerRound = playerRound,
            currentQuestionIndex = answeredCount,
            totalQuestions = questions.size,
            currentQuestion = currentQuestion,
            questionShownAt = Instant.now(),
            isCompleted = currentQuestion == null
        )
    }

    @Transactional
    fun submitAnswer(
        user: User,
        playerRoundId: Long,
        questionId: Long,
        answerId: Long?,
        questionShownAt: Instant
    ): AnswerResult? {
        val playerRound = playerRoundRepository.findById(playerRoundId).orElse(null) ?: return null
        if (playerRound.user.id != user.id) return null
        if (playerRound.isCompleted()) return null

        val question = questionRepository.findById(questionId).orElse(null) ?: return null

        // Check if already answered
        if (playerAnswerRepository.existsByPlayerRoundIdAndQuestionId(playerRoundId, questionId)) {
            return null
        }

        val selectedAnswer = if (answerId != null) {
            answerOptionRepository.findById(answerId).orElse(null)
        } else {
            null
        }

        val answeredAt = Instant.now()
        val correct = selectedAnswer?.correct == true
        val score = calculateScore(questionShownAt, answeredAt, correct)

        // Save answer
        val playerAnswer = PlayerAnswer(
            playerRound = playerRound,
            question = question,
            selectedAnswer = selectedAnswer,
            questionShownAt = questionShownAt,
            answeredAt = answeredAt,
            score = score
        )
        playerAnswerRepository.save(playerAnswer)

        // Update total score
        playerRound.totalScore += score
        playerRound.answers.add(playerAnswer)

        // Check if quiz is complete
        val questions = questionRepository.findByRoundIdOrderByOrderIndex(playerRound.round.id)
        val answeredCount = playerAnswerRepository.countByPlayerRoundId(playerRoundId)

        if (answeredCount >= questions.size) {
            playerRound.completedAt = Instant.now()
        }

        playerRoundRepository.save(playerRound)

        val correctAnswer = question.getCorrectAnswer()!!

        return AnswerResult(
            correct = correct,
            score = score,
            correctAnswer = correctAnswer,
            explanation = question.explanation,
            hasNextQuestion = answeredCount < questions.size
        )
    }

    fun calculateScore(shownAt: Instant, answeredAt: Instant, correct: Boolean): Int {
        if (!correct) return 0

        val durationMs = Duration.between(shownAt, answeredAt).toMillis()
        val seconds = durationMs / 1000.0

        return when {
            seconds <= 0 -> MAX_SCORE
            seconds >= TIMER_SECONDS -> MIN_SCORE
            else -> {
                // Linear interpolation: 100 at 0s, 50 at 10s
                val scoreRange = MAX_SCORE - MIN_SCORE
                val timeRatio = seconds / TIMER_SECONDS
                (MAX_SCORE - (scoreRange * timeRatio)).toInt()
            }
        }
    }

    fun getPlayerHistory(userId: Long): List<PlayerRound> {
        return playerRoundRepository.findByUserIdOrderByStartedAtDesc(userId)
    }

    fun getPlayerRound(id: Long): PlayerRound? {
        return playerRoundRepository.findById(id).orElse(null)
    }

    fun getPlayerRoundWithAnswers(id: Long): PlayerRound? {
        return playerRoundRepository.findByIdWithAnswers(id)
    }
}
