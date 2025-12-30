package ch.quizzing.squizzing.domain

import jakarta.persistence.*

@Entity
@Table(name = "answer_options")
class AnswerOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    val question: Question,

    @Column(nullable = false)
    var orderIndex: Int,

    @Column(columnDefinition = "TEXT")
    var text: String? = null,

    var imageFilename: String? = null,

    @Column(nullable = false)
    var correct: Boolean = false
)
