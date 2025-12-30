package ch.quizzing.squizzing.controller

import ch.quizzing.squizzing.repository.RoundRepository
import ch.quizzing.squizzing.service.AuthService
import ch.quizzing.squizzing.service.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val roundRepository: RoundRepository
) {

    @GetMapping("/")
    fun home(
        @AuthenticationPrincipal principal: UserPrincipal?,
        model: Model
    ): String {
        val activeRound = roundRepository.findByActiveTrue()
        model.addAttribute("activeRound", activeRound)
        model.addAttribute("user", principal?.user)
        return "home"
    }
}
