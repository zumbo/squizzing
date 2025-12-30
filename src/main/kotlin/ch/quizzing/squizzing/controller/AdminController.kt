package ch.quizzing.squizzing.controller

import ch.quizzing.squizzing.domain.UserRole
import ch.quizzing.squizzing.repository.QuestionRepository
import ch.quizzing.squizzing.service.*
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.LocalDate

@Controller
@RequestMapping("/admin")
class AdminController(
    private val roundService: RoundService,
    private val userService: UserService,
    private val questionImportService: QuestionImportService,
    private val questionRepository: QuestionRepository,
    private val imageStorageService: ImageStorageService
) {

    @GetMapping
    fun dashboard(model: Model): String {
        model.addAttribute("rounds", roundService.findAll())
        model.addAttribute("users", userService.findAll())
        return "admin/dashboard"
    }

    // ===== ROUNDS =====

    @GetMapping("/rounds")
    fun rounds(model: Model): String {
        val rounds = roundService.findAll()
        val questionCounts = rounds.associate { it.id to questionRepository.countByRoundId(it.id) }
        model.addAttribute("rounds", rounds)
        model.addAttribute("questionCounts", questionCounts)
        return "admin/rounds"
    }

    @GetMapping("/rounds/new")
    fun newRoundForm(model: Model): String {
        model.addAttribute("round", null)
        return "admin/round-form"
    }

    @PostMapping("/rounds")
    fun createRound(
        @RequestParam name: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        redirectAttributes: RedirectAttributes
    ): String {
        roundService.create(name, startDate, endDate)
        redirectAttributes.addFlashAttribute("success", "Round created successfully")
        return "redirect:/admin/rounds"
    }

    @GetMapping("/rounds/{id}/edit")
    fun editRoundForm(@PathVariable id: Long, model: Model): String {
        val round = roundService.findById(id) ?: return "redirect:/admin/rounds"
        model.addAttribute("round", round)
        return "admin/round-form"
    }

    @PostMapping("/rounds/{id}")
    fun updateRound(
        @PathVariable id: Long,
        @RequestParam name: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        redirectAttributes: RedirectAttributes
    ): String {
        roundService.update(id, name, startDate, endDate)
        redirectAttributes.addFlashAttribute("success", "Round updated successfully")
        return "redirect:/admin/rounds"
    }

    @PostMapping("/rounds/{id}/activate")
    fun activateRound(@PathVariable id: Long, redirectAttributes: RedirectAttributes): String {
        roundService.activate(id)
        redirectAttributes.addFlashAttribute("success", "Round activated")
        return "redirect:/admin/rounds"
    }

    @PostMapping("/rounds/{id}/deactivate")
    fun deactivateRound(@PathVariable id: Long, redirectAttributes: RedirectAttributes): String {
        roundService.deactivate(id)
        redirectAttributes.addFlashAttribute("success", "Round deactivated")
        return "redirect:/admin/rounds"
    }

    @PostMapping("/rounds/{id}/delete")
    fun deleteRound(@PathVariable id: Long, redirectAttributes: RedirectAttributes): String {
        roundService.delete(id)
        redirectAttributes.addFlashAttribute("success", "Round deleted")
        return "redirect:/admin/rounds"
    }

    // ===== QUESTIONS =====

    @GetMapping("/rounds/{id}/questions")
    fun questions(@PathVariable id: Long, model: Model): String {
        val round = roundService.findById(id) ?: return "redirect:/admin/rounds"
        model.addAttribute("round", round)
        model.addAttribute("questions", questionRepository.findByRoundIdOrderByOrderIndex(id))
        return "admin/questions"
    }

    @PostMapping("/rounds/{id}/questions/import")
    fun importQuestions(
        @PathVariable id: Long,
        @RequestParam file: MultipartFile,
        redirectAttributes: RedirectAttributes
    ): String {
        val round = roundService.findById(id)
        if (round == null) {
            redirectAttributes.addFlashAttribute("error", "Round not found")
            return "redirect:/admin/rounds"
        }

        val result = questionImportService.importQuestions(file, round)

        if (result.success) {
            redirectAttributes.addFlashAttribute("success", "Imported ${result.questionsImported} questions")
        } else {
            redirectAttributes.addFlashAttribute("error", "Import failed")
        }

        if (result.errors.isNotEmpty()) {
            redirectAttributes.addFlashAttribute("importErrors", result.errors)
        }

        return "redirect:/admin/rounds/${id}/questions"
    }

    @PostMapping("/questions/{id}/delete")
    fun deleteQuestion(
        @PathVariable id: Long,
        @RequestParam roundId: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        questionRepository.deleteById(id)
        redirectAttributes.addFlashAttribute("success", "Question deleted")
        return "redirect:/admin/rounds/${roundId}/questions"
    }

    // ===== USERS =====

    @GetMapping("/users")
    fun users(model: Model): String {
        model.addAttribute("users", userService.findAll())
        model.addAttribute("roles", UserRole.entries)
        return "admin/users"
    }

    @PostMapping("/users")
    fun createUser(
        @RequestParam email: String,
        @RequestParam displayName: String,
        @RequestParam role: UserRole,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            userService.create(email, displayName, role)
            redirectAttributes.addFlashAttribute("success", "User created successfully")
        } catch (e: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("error", e.message)
        }
        return "redirect:/admin/users"
    }

    @PostMapping("/users/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @RequestParam displayName: String,
        @RequestParam role: UserRole,
        redirectAttributes: RedirectAttributes
    ): String {
        userService.update(id, displayName, role)
        redirectAttributes.addFlashAttribute("success", "User updated successfully")
        return "redirect:/admin/users"
    }

    @PostMapping("/users/{id}/delete")
    fun deleteUser(@PathVariable id: Long, redirectAttributes: RedirectAttributes): String {
        userService.delete(id)
        redirectAttributes.addFlashAttribute("success", "User deleted")
        return "redirect:/admin/users"
    }
}
