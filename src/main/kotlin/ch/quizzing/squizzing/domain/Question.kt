package ch.quizzing.squizzing.domain

import jakarta.persistence.*

@Entity
@Table(name = "questions")
class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    val round: Round,

    @Column(nullable = false)
    var orderIndex: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    var language: UserLanguage = UserLanguage.DE,

    @Column(columnDefinition = "TEXT")
    var text: String? = null,

    var imageFilename: String? = null,

    @Column(columnDefinition = "TEXT")
    var explanation: String? = null,

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val answerOptions: MutableSet<AnswerOption> = mutableSetOf()
) {
    fun getCorrectAnswer(): AnswerOption? = answerOptions.find { it.correct }
}
