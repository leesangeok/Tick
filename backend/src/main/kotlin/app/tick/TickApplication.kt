package app.tick

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TickApplication

fun main(args: Array<String>) {
	runApplication<TickApplication>(*args)
}
