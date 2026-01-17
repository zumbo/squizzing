package ch.quizzing.squizzing.controller

import ch.quizzing.squizzing.domain.UserLanguage
import ch.quizzing.squizzing.domain.UserRole
import ch.quizzing.squizzing.repository.QuestionRepository
import ch.quizzing.squizzing.service.*
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
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
        model.addAttribute("languages", UserLanguage.entries)
        return "admin/questions"
    }

    @PostMapping("/rounds/{id}/questions/import")
    fun importQuestions(
        @PathVariable id: Long,
        @RequestParam file: MultipartFile,
        @RequestParam language: UserLanguage,
        redirectAttributes: RedirectAttributes
    ): String {
        val round = roundService.findById(id)
        if (round == null) {
            redirectAttributes.addFlashAttribute("error", "Round not found")
            return "redirect:/admin/rounds"
        }

        val result = questionImportService.importQuestions(file, round, language)

        if (result.success) {
            redirectAttributes.addFlashAttribute("success", "Imported ${result.questionsImported} ${language.displayName} questions")
        } else {
            redirectAttributes.addFlashAttribute("error", "Import failed")
        }

        if (result.errors.isNotEmpty()) {
            redirectAttributes.addFlashAttribute("importErrors", result.errors)
        }

        return "redirect:/admin/rounds/${id}/questions"
    }

    @PostMapping("/rounds/{id}/images/upload")
    fun uploadImages(
        @PathVariable id: Long,
        @RequestParam imageFiles: List<MultipartFile>,
        redirectAttributes: RedirectAttributes
    ): String {
        val round = roundService.findById(id)
        if (round == null) {
            redirectAttributes.addFlashAttribute("error", "Round not found")
            return "redirect:/admin/rounds"
        }

        val uploadedFilenames = imageFiles
            .filter { !it.isEmpty }
            .map { imageStorageService.store(it, "questions") }

        if (uploadedFilenames.isNotEmpty()) {
            redirectAttributes.addFlashAttribute("uploadedImages", uploadedFilenames)
            redirectAttributes.addFlashAttribute("success", "Uploaded ${uploadedFilenames.size} image(s)")
        } else {
            redirectAttributes.addFlashAttribute("error", "No images were uploaded")
        }

        return "redirect:/admin/rounds/${id}/questions"
    }

    @PostMapping("/questions/{id}")
    @Transactional
    fun updateQuestion(
        @PathVariable id: Long,
        @RequestParam roundId: Long,
        @RequestParam text: String?,
        @RequestParam explanation: String?,
        @RequestParam answer1: String,
        @RequestParam answer2: String,
        @RequestParam answer3: String,
        @RequestParam answer4: String,
        @RequestParam answerId1: Long,
        @RequestParam answerId2: Long,
        @RequestParam answerId3: Long,
        @RequestParam answerId4: Long,
        @RequestParam correctAnswer: Int,
        @RequestParam(required = false) imageUrl: String?,
        @RequestParam(required = false) imageFile: MultipartFile?,
        @RequestParam(required = false) removeImage: Boolean?,
        redirectAttributes: RedirectAttributes
    ): String {
        val question = questionRepository.findById(id).orElse(null)
        if (question == null) {
            redirectAttributes.addFlashAttribute("error", "Question not found")
            return "redirect:/admin/rounds/${roundId}/questions"
        }

        question.text = text?.takeIf { it.isNotBlank() }
        question.explanation = explanation?.takeIf { it.isNotBlank() }

        // Handle image updates
        if (removeImage == true) {
            // Delete old local file if it exists and is not a URL
            question.imageFilename?.let { oldFilename ->
                if (!oldFilename.startsWith("http")) {
                    imageStorageService.delete(oldFilename)
                }
            }
            question.imageFilename = null
        } else if (imageFile != null && !imageFile.isEmpty) {
            // Delete old local file if it exists and is not a URL
            question.imageFilename?.let { oldFilename ->
                if (!oldFilename.startsWith("http")) {
                    imageStorageService.delete(oldFilename)
                }
            }
            // Store new file
            question.imageFilename = imageStorageService.store(imageFile, "questions")
        } else if (!imageUrl.isNullOrBlank()) {
            // Delete old local file if it exists and is not a URL
            question.imageFilename?.let { oldFilename ->
                if (!oldFilename.startsWith("http")) {
                    imageStorageService.delete(oldFilename)
                }
            }
            // Use the URL directly
            question.imageFilename = imageUrl
        }

        val answers = mapOf(
            answerId1 to Pair(answer1, correctAnswer == 1),
            answerId2 to Pair(answer2, correctAnswer == 2),
            answerId3 to Pair(answer3, correctAnswer == 3),
            answerId4 to Pair(answer4, correctAnswer == 4)
        )

        question.answerOptions.forEach { option ->
            answers[option.id]?.let { (text, correct) ->
                option.text = text
                option.correct = correct
            }
        }

        questionRepository.save(question)
        redirectAttributes.addFlashAttribute("success", "Question updated")
        return "redirect:/admin/rounds/${roundId}/questions"
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
        model.addAttribute("languages", UserLanguage.entries)
        return "admin/users"
    }

    @PostMapping("/users")
    fun createUser(
        @RequestParam email: String,
        @RequestParam displayName: String,
        @RequestParam role: UserRole,
        @RequestParam language: UserLanguage,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            userService.create(email, displayName, role, language)
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
        @RequestParam language: UserLanguage,
        redirectAttributes: RedirectAttributes
    ): String {
        userService.update(id, displayName, role, language)
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
