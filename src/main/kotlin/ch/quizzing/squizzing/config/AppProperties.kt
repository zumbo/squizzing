package ch.quizzing.squizzing.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "squizzing")
data class AppProperties(
    val uploadDir: String = "./uploads",
    val baseUrl: String = "http://localhost:8080",
    val magicLink: MagicLinkProperties = MagicLinkProperties()
) {
    data class MagicLinkProperties(
        val expiryMinutes: Long = 15
    )
}
