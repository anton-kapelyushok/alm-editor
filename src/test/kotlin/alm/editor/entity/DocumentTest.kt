package alm.editor.entity

import assertk.assert
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class DocumentTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `weird entities serialization works`() {
        val dto = objectMapper.readValue<LineUpdate>("""
            {
                "type": "add-symbols",
                "index": 4,
                "symbols": "poupa"
            }
        """.trimIndent())

        assert(dto).isEqualTo(AddSymbols(4, "poupa"))
    }
}
