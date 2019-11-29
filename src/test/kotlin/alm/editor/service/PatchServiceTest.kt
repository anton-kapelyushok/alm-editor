package alm.editor.service

import alm.editor.entity.*
import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(value = [MockKExtension::class])
internal class PatchServiceTest {

    @InjectMockKs
    private lateinit var patchService: PatchService

    private lateinit var document: Document

    @Test
    fun `should add lines to empty document`() {
        // given
        document = emptyDocument("1")

        // when
        document = patchService.applyPatch(document, Patch(
                id = "patchId",
                revision = 0,
                updates = listOf(
                        ChangeLineUpdate(0, AddSymbols(0, "Hello")),
                        AddLineUpdate(1),
                        ChangeLineUpdate(1, AddSymbols(0, "World"))
                )
        ))

        // then
        assert(document.state).isEqualTo("""
            Hello
            World
        """.trimIndent())
    }

    @Test
    fun `should merge non conflicting patches`() {
        // given
        document = emptyDocument("1")

        // when
        document = patchService.applyPatch(document, Patch(
                id = "patchId1",
                revision = 0,
                updates = listOf(
                        ChangeLineUpdate(0, AddSymbols(0, "Hello"))
                )
        ))


        document = patchService.applyPatch(document, Patch(
                id = "patchId2",
                revision = 1,
                updates = listOf(
                        AddLineUpdate(1),
                        ChangeLineUpdate(1, AddSymbols(0, "World"))
                )
        ))

        // then
        assert(document.state).isEqualTo("""
            Hello
            World
        """.trimIndent())

        assert(document.commits).hasSize(3)
        assert(document.commits[1].patchId).isEqualTo("patchId1")
        assert(document.commits[2].patchId).isEqualTo("patchId2")
    }

    @Test
    fun `should merge conflicting patches`() {
        // given
        document = emptyDocument("1")

        // when
        document = patchService.applyPatch(document, Patch(
                id = "patchId1",
                revision = 0,
                updates = listOf(
                        ChangeLineUpdate(0, AddSymbols(0, "Hello"))
                )
        ))


        document = patchService.applyPatch(document, Patch(
                id = "patchId2",
                revision = 0,
                updates = listOf(
                        AddLineUpdate(1),
                        ChangeLineUpdate(1, AddSymbols(0, "World"))
                )
        ))

        // then
        assert(document.state).isEqualTo("""
            Hello
            World
        """.trimIndent())

        assert(document.commits).hasSize(3)
    }

    @Test
    fun `should merge conflicts with some heavy line moving`() {
        // given
        document = emptyDocument("1")

        // when
        document = patchService.applyPatch(document, Patch(
                id = "patchId1",
                revision = 0,
                updates = listOf(
                        ChangeLineUpdate(0, AddSymbols(0, "He")),
                        AddLineUpdate(0),
                        ChangeLineUpdate(1, AddSymbols(2, "ll")),
                        AddLineUpdate(1),
                        ChangeLineUpdate(2, AddSymbols(4, "oo")),
                        RemoveLineUpdate(0),
                        ChangeLineUpdate(1, RemoveSymbols(5, 5)),
                        RemoveLineUpdate(0)
                )
        ))

        document = patchService.applyPatch(document, Patch(
                id = "patchId2",
                revision = 0,
                updates = listOf(
                        AddLineUpdate(0),
                        ChangeLineUpdate(1, AddSymbols(0, ", ")),
                        AddLineUpdate(0),
                        ChangeLineUpdate(2, AddSymbols(2, "World")),
                        RemoveLineUpdate(1),
                        RemoveLineUpdate(0)
                )
        ))

        // then
        assert(document.state).isEqualTo("""
            Hello, World
        """.trimIndent())

        assert(document.commits).hasSize(3)
    }
}
