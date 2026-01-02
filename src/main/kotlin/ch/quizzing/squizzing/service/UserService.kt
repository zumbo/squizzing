package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.domain.User
import ch.quizzing.squizzing.domain.UserLanguage
import ch.quizzing.squizzing.domain.UserRole
import ch.quizzing.squizzing.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun findAll(): List<User> = userRepository.findAll()

    fun findById(id: Long): User? = userRepository.findById(id).orElse(null)

    fun findByEmail(email: String): User? = userRepository.findByEmail(email.lowercase().trim())

    @Transactional
    fun create(email: String, displayName: String, role: UserRole = UserRole.PLAYER, language: UserLanguage = UserLanguage.DE): User {
        val normalizedEmail = email.lowercase().trim()

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw IllegalArgumentException("User with email $normalizedEmail already exists")
        }

        val user = User(
            email = normalizedEmail,
            displayName = displayName.trim(),
            role = role,
            language = language
        )
        return userRepository.save(user)
    }

    @Transactional
    fun update(id: Long, displayName: String, role: UserRole, language: UserLanguage): User? {
        val user = userRepository.findById(id).orElse(null) ?: return null
        user.displayName = displayName.trim()
        user.role = role
        user.language = language
        return userRepository.save(user)
    }

    @Transactional
    fun delete(id: Long) {
        userRepository.deleteById(id)
    }
}
