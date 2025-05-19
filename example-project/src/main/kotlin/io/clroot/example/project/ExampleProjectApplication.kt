package io.clroot.example.project

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ExampleProjectApplication

fun main(args: Array<String>) {
    val application = SpringApplication(ExampleProjectApplication::class.java)
    application.run(*args)
}