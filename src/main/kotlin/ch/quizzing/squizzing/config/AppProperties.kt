package ch.quizzing.squizzing.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "squizzing")
data class AppProperties(
    val uploadDir: String = "./uploads",
    val baseUrl: String = "http://localhost:8080",
    val magicLink: MagicLinkProperties = MagicLinkProperties(),
    val scoring: ScoringProperties = ScoringProperties()
) {
    data class MagicLinkProperties(
        val expiryMinutes: Long = 15
    )

    data class ScoringProperties(
        val maxScore: Int = 100,
        val minScore: Int = 50,
        val fullScoreSeconds: Double = 3.0,
        val minScoreSeconds: Double = 20.0
    )
}
