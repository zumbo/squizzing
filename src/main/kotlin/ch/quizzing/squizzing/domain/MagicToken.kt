package ch.quizzing.squizzing.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "magic_tokens")
class MagicToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val token: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    var used: Boolean = false,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
