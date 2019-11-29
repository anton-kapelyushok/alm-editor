package alm.editor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EditorApplication

fun main(args: Array<String>) {
    runApplication<EditorApplication>(*args)
}

