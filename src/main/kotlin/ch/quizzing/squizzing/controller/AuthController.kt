package ch.quizzing.squizzing.controller

import ch.quizzing.squizzing.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @GetMapping("/login")
    fun loginPage(): String {
        return "auth/login"
    }

    @PostMapping("/magic-link")
    fun requestMagicLink(
        @RequestParam email: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val success = authService.requestMagicLink(email)

        // Always show the same message to prevent email enumeration
        redirectAttributes.addFlashAttribute("message",
            "If an account exists for $email, you will receive a login link shortly.")

        return "redirect:/auth/check-email"
    }

    @GetMapping("/check-email")
    fun checkEmailPage(): String {
        return "auth/check-email"
    }

    @GetMapping("/verify")
    fun verifyMagicLink(
        @RequestParam token: String,
        request: HttpServletRequest,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = authService.verifyMagicLink(token)

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired link. Please request a new one.")
            return "redirect:/auth/login"
        }

        // Authenticate the user
        authService.authenticateUser(user)

        // Save security context to session
        val session: HttpSession = request.getSession(true)
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            org.springframework.security.core.context.SecurityContextHolder.getContext()
        )

        redirectAttributes.addFlashAttribute("success", "Welcome back, ${user.displayName}!")
        return "redirect:/"
    }
}
