package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendMagicLink(email: String, token: String) {
        val magicLinkUrl = "${appProperties.baseUrl}/auth/verify?token=$token"

        val message = SimpleMailMessage().apply {
            setTo(email)
            subject = "Squizzing - Login Link"
            text = """
                Hello!

                Click the link below to log in to Squizzing:

                $magicLinkUrl

                This link will expire in ${appProperties.magicLink.expiryMinutes} minutes.

                If you didn't request this link, you can safely ignore this email.
            """.trimIndent()
        }

        try {
            mailSender.send(message)
            log.info("Magic link email sent to: {}", email)
        } catch (e: Exception) {
            // During development, log the link if email sending fails
            log.warn("Failed to send email to {}. Magic link URL: {}", email, magicLinkUrl)
            log.info("=== DEVELOPMENT MODE: Magic Link for {} ===", email)
            log.info(magicLinkUrl)
            log.info("===========================================")
        }
    }
}
