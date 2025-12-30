package ch.quizzing.squizzing.domain

import jakarta.persistence.*
import java.time.Instant

enum class UserRole {
    PLAYER,
    ADMIN
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

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
