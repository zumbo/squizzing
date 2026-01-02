package ch.quizzing.squizzing.domain

import jakarta.persistence.*
import java.time.Instant

enum class UserRole {
    PLAYER,
    ADMIN
}

enum class UserLanguage(val code: String, val displayName: String) {
    DE("de", "Deutsch"),
    EN("en", "English")
}

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var displayName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.PLAYER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    var language: UserLanguage = UserLanguage.DE,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
