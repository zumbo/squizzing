package ch.quizzing.squizzing.controller

import ch.quizzing.squizzing.service.QuizService
import ch.quizzing.squizzing.service.RoundService
import ch.quizzing.squizzing.service.UserPrincipal
import jakarta.servlet.http.HttpSession
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Instant

@Controller
@RequestMapping("/quiz")
class QuizController(
    private val quizService: QuizService,
    private val roundService: RoundService
) {

    @GetMapping
    fun quizHome(
        @AuthenticationPrincipal principal: UserPrincipal,
        model: Model
    ): String {
        val activeRound = roundService.findActiveRound()
        model.addAttribute("activeRound", activeRound)

        if (activeRound != null) {
            model.addAttribute("canPlay", quizService.canPlayRound(principal.id, activeRound.id))
            model.addAttribute("hasCompleted", quizService.hasCompletedRound(principal.id, activeRound.id))
        }

        return "quiz/home"
    }

    @GetMapping("/start/{roundId}")
    fun startQuiz(
        @PathVariable roundId: Long,
        @AuthenticationPrincipal principal: UserPrincipal,
        session: HttpSession,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        if (!quizService.canPlayRound(principal.id, roundId)) {
            if (quizService.hasCompletedRound(principal.id, roundId)) {
                redirectAttributes.addFlashAttribute("error", "You have already completed this quiz.")
            } else {
                redirectAttributes.addFlashAttribute("error", "This quiz is not available.")
            }
            return "redirect:/"
        }

        val quizState = quizService.startOrResumeQuiz(principal.user, roundId)
        if (quizState == null) {
            redirectAttributes.addFlashAttribute("error", "Could not start quiz.")
            return "redirect:/"
        }

        if (quizState.isCompleted) {
            return "redirect:/quiz/result/${quizState.playerRound.id}"
        }

        // Store question shown timestamp in session
        session.setAttribute("questionShownAt", quizState.questionShownAt)

        model.addAttribute("state", quizState)
        model.addAttribute("question", quizState.currentQuestion)
        model.addAttribute("shuffledAnswers", quizState.currentQuestion!!.answerOptions.shuffled())
        model.addAttribute("questionNumber", quizState.currentQuestionIndex + 1)
        model.addAttribute("totalQuestions", quizState.totalQuestions)
        model.addAttribute("timerSeconds", QuizService.TIMER_SECONDS)

        return "quiz/question"
    }

    @PostMapping("/answer")
    fun submitAnswer(
        @RequestParam playerRoundId: Long,
        @RequestParam questionId: Long,
        @RequestParam(required = false) answerId: Long?,
        @AuthenticationPrincipal principal: UserPrincipal,
        session: HttpSession,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        val questionShownAt = session.getAttribute("questionShownAt") as? Instant
            ?: Instant.now().minusSeconds(QuizService.TIMER_SECONDS.toLong())

        val result = quizService.submitAnswer(
            user = principal.user,
            playerRoundId = playerRoundId,
            questionId = questionId,
            answerId = answerId,
            questionShownAt = questionShownAt
        )

        if (result == null) {
            redirectAttributes.addFlashAttribute("error", "Could not submit answer.")
            return "redirect:/"
        }

        // Clear session timestamp
        session.removeAttribute("questionShownAt")

        model.addAttribute("result", result)
        model.addAttribute("playerRoundId", playerRoundId)

        return "quiz/answer-result"
    }

    @GetMapping("/continue/{playerRoundId}")
    fun continueQuiz(
        @PathVariable playerRoundId: Long,
        @AuthenticationPrincipal principal: UserPrincipal,
        session: HttpSession,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        val playerRound = quizService.getPlayerRound(playerRoundId)
        if (playerRound == null || playerRound.user.id != principal.id) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found.")
            return "redirect:/"
        }

        if (playerRound.isCompleted()) {
            return "redirect:/quiz/result/$playerRoundId"
        }

        // Resume quiz from where we left off
        val quizState = quizService.startOrResumeQuiz(principal.user, playerRound.round.id)
        if (quizState == null || quizState.currentQuestion == null) {
            return "redirect:/quiz/result/$playerRoundId"
        }

        // Store question shown timestamp in session
        session.setAttribute("questionShownAt", Instant.now())

        model.addAttribute("state", quizState)
        model.addAttribute("question", quizState.currentQuestion)
        model.addAttribute("shuffledAnswers", quizState.currentQuestion!!.answerOptions.shuffled())
        model.addAttribute("questionNumber", quizState.currentQuestionIndex + 1)
        model.addAttribute("totalQuestions", quizState.totalQuestions)
        model.addAttribute("timerSeconds", QuizService.TIMER_SECONDS)

        return "quiz/question"
    }

    @GetMapping("/result/{playerRoundId}")
    fun quizResult(
        @PathVariable playerRoundId: Long,
        @AuthenticationPrincipal principal: UserPrincipal,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        val playerRound = quizService.getPlayerRoundWithAnswers(playerRoundId)
        if (playerRound == null || playerRound.user.id != principal.id) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found.")
            return "redirect:/"
        }

        val answers = playerRound.answers.sortedBy { it.question.orderIndex }
        model.addAttribute("playerRound", playerRound)
        model.addAttribute("answers", answers)
        model.addAttribute("totalQuestions", answers.size)
        model.addAttribute("correctCount", answers.count { it.isCorrect() })
        model.addAttribute("incorrectCount", answers.count { !it.isCorrect() })

        return "quiz/result"
    }

    @GetMapping("/history")
    fun quizHistory(
        @AuthenticationPrincipal principal: UserPrincipal,
        model: Model
    ): String {
        val history = quizService.getPlayerHistory(principal.id)
        model.addAttribute("history", history)
        return "quiz/history"
    }
}
