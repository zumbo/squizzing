package ch.quizzing.squizzing.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "player_answers")
class PlayerAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_round_id", nullable = false)
    val playerRound: PlayerRound,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    val question: Question,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    val selectedAnswer: AnswerOption? = null,

    @Column(nullable = false)
    val questionShownAt: Instant,

    @Column(nullable = false)
    val answeredAt: Instant = Instant.now(),

    @Column(nullable = false)
    val score: Int = 0
) {
    fun isCorrect(): Boolean = selectedAnswer?.correct == true
}
