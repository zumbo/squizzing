package ch.quizzing.squizzing

import ch.quizzing.squizzing.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class SquizzingApplication

fun main(args: Array<String>) {
	runApplication<SquizzingApplication>(*args)
}
