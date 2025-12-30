package ch.quizzing.squizzing.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.Instant

@Entity
@Table(name = "rounds")
class Round(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var endDate: LocalDate,

    @Column(nullable = false)
    var active: Boolean = false,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "round", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val questions: MutableList<Question> = mutableListOf()
)
