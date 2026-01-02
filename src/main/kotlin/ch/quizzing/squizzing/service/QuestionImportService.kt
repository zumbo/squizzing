package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.domain.AnswerOption
import ch.quizzing.squizzing.domain.Question
import ch.quizzing.squizzing.domain.Round
import ch.quizzing.squizzing.domain.UserLanguage
import ch.quizzing.squizzing.repository.QuestionRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader

data class ImportResult(
    val success: Boolean,
    val questionsImported: Int,
    val errors: List<String>
)

@Service
class QuestionImportService(
    private val questionRepository: QuestionRepository
) {

    private fun getExistingQuestionCount(roundId: Long, language: UserLanguage): Int {
        return questionRepository.countByRoundIdAndLanguage(roundId, language).toInt()
    }

    private val log = LoggerFactory.getLogger(QuestionImportService::class.java)

    /**
     * Import questions from an Excel or CSV file.
     *
     * Expected format (columns):
     * 1. Question Text
     * 2. Question Type (ignored, assumed Multiple Choice)
     * 3. Option 1
     * 4. Option 2
     * 5. Option 3
     * 6. Option 4
     * 7. Correct Answer (1-4)
     * 8. Time in seconds (ignored, we use fixed 10s)
     * 9. Image Link (optional)
     * 10. Explanation (optional)
     */
    @Transactional
    fun importQuestions(file: MultipartFile, round: Round, language: UserLanguage): ImportResult {
        val filename = file.originalFilename ?: "unknown"
        val errors = mutableListOf<String>()

        return try {
            val questions = if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                parseExcel(file, round, language, errors)
            } else if (filename.endsWith(".csv")) {
                parseCsv(file, round, language, errors)
            } else {
                return ImportResult(false, 0, listOf("Unsupported file format. Please use .xlsx, .xls, or .csv"))
            }

            if (questions.isEmpty() && errors.isEmpty()) {
                return ImportResult(false, 0, listOf("No questions found in file"))
            }

            // Save questions
            questionRepository.saveAll(questions)

            log.info("Imported {} {} questions for round {}", questions.size, language.code, round.id)
            ImportResult(errors.isEmpty(), questions.size, errors)
        } catch (e: Exception) {
            log.error("Error importing questions", e)
            ImportResult(false, 0, listOf("Error reading file: ${e.message}"))
        }
    }

    private fun parseExcel(file: MultipartFile, round: Round, language: UserLanguage, errors: MutableList<String>): List<Question> {
        val questions = mutableListOf<Question>()
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)

        var orderIndex = getExistingQuestionCount(round.id, language)

        for (rowIndex in 1..sheet.lastRowNum) { // Skip header row
            val row = sheet.getRow(rowIndex) ?: continue

            try {
                val question = parseRow(row, round, language, orderIndex, errors, rowIndex)
                if (question != null) {
                    questions.add(question)
                    orderIndex++
                }
            } catch (e: Exception) {
                errors.add("Row ${rowIndex + 1}: ${e.message}")
            }
        }

        workbook.close()
        return questions
    }

    private fun parseCsv(file: MultipartFile, round: Round, language: UserLanguage, errors: MutableList<String>): List<Question> {
        val questions = mutableListOf<Question>()
        val reader = BufferedReader(InputStreamReader(file.inputStream))

        var orderIndex = getExistingQuestionCount(round.id, language)
        var lineNumber = 0

        reader.useLines { lines ->
            lines.drop(1).forEach { line -> // Skip header
                lineNumber++
                try {
                    val columns = parseCsvLine(line)
                    val question = parseColumns(columns, round, language, orderIndex, errors, lineNumber)
                    if (question != null) {
                        questions.add(question)
                        orderIndex++
                    }
                } catch (e: Exception) {
                    errors.add("Line ${lineNumber + 1}: ${e.message}")
                }
            }
        }

        return questions
    }

    private fun parseRow(row: Row, round: Round, language: UserLanguage, orderIndex: Int, errors: MutableList<String>, rowIndex: Int): Question? {
        val columns = (0..9).map { getCellValue(row, it) }
        return parseColumns(columns, round, language, orderIndex, errors, rowIndex)
    }

    private fun parseColumns(columns: List<String>, round: Round, language: UserLanguage, orderIndex: Int, errors: MutableList<String>, lineNumber: Int): Question? {
        if (columns.isEmpty() || columns.all { it.isBlank() }) {
            return null // Skip empty rows
        }

        // Column 0: Question Text
        val questionText = columns.getOrNull(0)?.takeIf { it.isNotBlank() }
        // Column 1: Question Type (ignored)
        // Column 8: Image Link
        val questionImage = columns.getOrNull(8)?.takeIf { it.isNotBlank() }

        if (questionText.isNullOrBlank() && questionImage.isNullOrBlank()) {
            errors.add("Row ${lineNumber + 1}: Question must have text or image")
            return null
        }

        // Column 6: Correct Answer (1-4)
        val correctAnswerStr = columns.getOrNull(6)
        val correctAnswer = correctAnswerStr?.toIntOrNull()

        if (correctAnswer == null || correctAnswer !in 1..4) {
            errors.add("Row ${lineNumber + 1}: Correct answer must be 1-4, got: $correctAnswerStr")
            return null
        }

        // Column 9: Explanation
        val explanation = columns.getOrNull(9)?.takeIf { it.isNotBlank() }

        val question = Question(
            round = round,
            orderIndex = orderIndex,
            language = language,
            text = questionText,
            imageFilename = questionImage,
            explanation = explanation
        )

        // Columns 2-5: Options 1-4
        for (i in 0..3) {
            val answerText = columns.getOrNull(2 + i)?.trim()?.takeIf { it.isNotBlank() }

            if (answerText.isNullOrBlank()) {
                errors.add("Row ${lineNumber + 1}: Option ${i + 1} is empty")
                return null
            }

            val answerOption = AnswerOption(
                question = question,
                orderIndex = i,
                text = answerText,
                imageFilename = null,
                correct = (i + 1 == correctAnswer)
            )
            question.answerOptions.add(answerOption)
        }

        return question
    }

    private fun getCellValue(row: Row, index: Int): String {
        val cell = row.getCell(index) ?: return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> cell.numericCellValue.toInt().toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())

        return result
    }
}
