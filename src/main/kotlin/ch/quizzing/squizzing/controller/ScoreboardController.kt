package ch.quizzing.squizzing.controller

import ch.quizzing.squizzing.service.ScoreboardService
import ch.quizzing.squizzing.service.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/scoreboard")
class ScoreboardController(
    private val scoreboardService: ScoreboardService
) {

    @GetMapping
    fun scoreboard(
        @RequestParam(required = false) roundId: Long?,
        @AuthenticationPrincipal principal: UserPrincipal?,
        model: Model
    ): String {
        val rounds = scoreboardService.getAvailableRounds()
        model.addAttribute("rounds", rounds)

        // Default to active round or first available
        val selectedRoundId = roundId
            ?: scoreboardService.getActiveRound()?.id
            ?: rounds.firstOrNull()?.id

        if (selectedRoundId != null) {
            val round = scoreboardService.findRound(selectedRoundId)
            val scoreboard = scoreboardService.getScoreboard(selectedRoundId, principal?.id)

            model.addAttribute("selectedRound", round)
            model.addAttribute("scoreboard", scoreboard)
        }

        return "scoreboard"
    }
}
