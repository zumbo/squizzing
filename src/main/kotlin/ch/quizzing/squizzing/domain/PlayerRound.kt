package ch.quizzing.squizzing.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "player_rounds",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "round_id"])]
)
class PlayerRound(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    val round: Round,

    @Column(nullable = false)
    val startedAt: Instant = Instant.now(),

    var completedAt: Instant? = null,

    @Column(nullable = false)
    var totalScore: Int = 0,

    var currentQuestionShownAt: Instant? = null,

    @OneToMany(mappedBy = "playerRound", cascade = [CascadeType.ALL], orphanRemoval = true)
    val answers: MutableSet<PlayerAnswer> = mutableSetOf()
) {
    fun isCompleted(): Boolean = completedAt != null
}
