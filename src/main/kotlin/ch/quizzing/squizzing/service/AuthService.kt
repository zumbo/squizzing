package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.config.AppProperties
import ch.quizzing.squizzing.domain.MagicToken
import ch.quizzing.squizzing.domain.User
import ch.quizzing.squizzing.domain.UserRole
import ch.quizzing.squizzing.repository.MagicTokenRepository
import ch.quizzing.squizzing.repository.UserRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val magicTokenRepository: MagicTokenRepository,
    private val emailService: EmailService,
    private val appProperties: AppProperties
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun requestMagicLink(email: String): Boolean {
        val normalizedEmail = email.lowercase().trim()

        // Find or create user
        val user = userRepository.findByEmail(normalizedEmail)
            ?: return false // User must exist to receive magic link

        // Generate token
        val token = generateSecureToken()
        val expiresAt = Instant.now().plusSeconds(appProperties.magicLink.expiryMinutes * 60)

        val magicToken = MagicToken(
            token = token,
            user = user,
            expiresAt = expiresAt
        )
        magicTokenRepository.save(magicToken)

        // Send email
        emailService.sendMagicLink(normalizedEmail, token)

        return true
    }

    @Transactional
    fun verifyMagicLink(token: String): User? {
        val magicToken = magicTokenRepository.findByToken(token) ?: return null

        // Check if token is valid
        if (magicToken.used) return null
        if (magicToken.expiresAt.isBefore(Instant.now())) return null

        // Mark token as used
        magicToken.used = true
        magicTokenRepository.save(magicToken)

        return magicToken.user
    }

    fun authenticateUser(user: User) {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val authentication = UsernamePasswordAuthenticationToken(
            UserPrincipal(user),
            null,
            authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    @Transactional
    fun cleanupExpiredTokens() {
        magicTokenRepository.deleteExpiredAndUsedTokens(Instant.now())
    }
}

data class UserPrincipal(
    val user: User
) {
    val id: Long get() = user.id
    val email: String get() = user.email
    val displayName: String get() = user.displayName
    val role: UserRole get() = user.role
}
