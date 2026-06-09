package app.tick

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TickApplication

fun main(args: Array<String>) {
	runApplication<TickApplication>(*args)
}
