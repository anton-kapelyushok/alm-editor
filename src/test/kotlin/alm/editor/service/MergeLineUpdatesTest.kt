package alm.editor.service

import alm.editor.entity.AddSymbols
import alm.editor.entity.LineUpdate
import alm.editor.entity.RemoveSymbols
import alm.editor.repository.DocumentRepository
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import assertk.assert
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(value = [MockKExtension::class])
internal class MergeLineUpdatesTest {

    val string = "0123456789"

    @MockK
    private lateinit var documentRepository: DocumentRepository

    @InjectMockKs
    private lateinit var patchService: PatchService


    @Test
    fun `should merge doc add to the left of patch add`() {
        test(add(1, "ab"), add(4, "cd"), "0ab123cd456789")
    }

    @Test
    fun `should merge doc add to the right of patch add`() {
        test(add(4, "ab"), add(1, "cd"), "0cd123ab456789")
    }

    @Test
    fun `should merge doc add at the same index as patch add`() {
        test(add(4, "ab"), add(4, "cd"), "0123abcd456789")
    }

    @Test
    fun `should merge doc add to the left of patch remove`() {
        test(add(1, "ab"), remove("4567"), "0ab12389")
    }

    @Test
    fun `should merge doc add inside patch remove`() {
        test(add(6, "ab"), remove("4567"), "012389")
    }

    @Test
    fun `should merge doc add to the right of patch remove`() {
        test(add(8, "ab"), remove("4567"), "0123ab89")
    }

    @Test
    fun `should merge doc remove containing patch add`() {
        test(remove("4567"), add(6, "ab"), "012389")
    }

    @Test
    fun `should merge doc remove to the left of patch add`() {
        test(remove("234"), add(6, "ab"), "015ab6789")
    }

    @Test
    fun `should merge doc remove to the right of patch add`() {
        test(remove("678"), add(3, "ab"), "012ab3459")
    }

    @Test
    fun `should merge doc remove to the left of patch remove`() {
        test(remove("234"), remove("678"), "0159")
    }

    @Test
    fun `should merge doc remove to the right of patch remove`() {
        test(remove("678"), remove("234"), "0159")
    }

    @Test
    fun `should merge doc remove containing patch remove`() {
        test(remove("2345678"), remove("456"), "019")
    }

    @Test
    fun `should merge doc remove contained in patch remove`() {
        test(remove("456"), remove("2345678"), "019")
    }

    @Test
    fun `should merge doc remove which has intersecting left part with patch remove`() {
        test(remove("2345"), remove("4567"), "0189")
    }

    @Test
    fun `should merge doc remove which has intersecting right part with patch remove`() {
        test(remove("4567"), remove("2345"), "0189")
    }

    private fun add(index: Int, symbols: String) = AddSymbols(index, symbols)
    private fun remove(symbols: String) = RemoveSymbols(
            fromIndex = Character.getNumericValue(symbols[0].toInt()),
            toIndex = Character.getNumericValue(symbols.last()))

    private fun test(docUpdate: LineUpdate, patchUpdate: LineUpdate, expected: String) {
        val merged = patchService.mergeLineUpdate(patchUpdate, docUpdate)

        fun apply(s: String, update: LineUpdate) = patchService.applyLineUpdate(s, update)

        assert(string
                .let { s -> apply(s, docUpdate) }
                .let { s ->
                    merged?.let { apply(s, it) } ?: s
                })
                .isEqualTo(expected)
    }
}
