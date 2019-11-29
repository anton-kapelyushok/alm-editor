package alm.editor.service

import alm.editor.entity.*
import org.springframework.stereotype.Service

@Service
class PatchService {
    fun applyPatch(document: Document, patch: Patch): Document {
        val serverUpdates = document.commits
                .slice(patch.revision + 1 until document.commits.size)
                .flatMap { it.updates }

        val movedPatchUpdates = patch.updates.mapNotNull { moveUpdate(it, serverUpdates) }
        val movedServerLineUpdates = serverUpdates
                .withIndex()
                .filter { (_, update) -> update is ChangeLineUpdate }
                .mapNotNull { (i, update) -> moveUpdate(update, serverUpdates.slice(i + 1 until serverUpdates.size) + movedPatchUpdates) }
                .filterIsInstance<ChangeLineUpdate>()

        val movedPatchLineUpdates = movedPatchUpdates
                .withIndex()
                .filter { (_, update) -> update is ChangeLineUpdate }
                .mapNotNull { (i, update) -> moveUpdate(update, movedPatchUpdates.slice(i + 1 until movedPatchUpdates.size)) }
                .filterIsInstance<ChangeLineUpdate>()

        val updates = merge(movedPatchUpdates.filter { it !is ChangeLineUpdate } + movedPatchLineUpdates, movedServerLineUpdates)

        return document.copy(
                state = applyUpdates(document.state, updates),
                commits = document.commits + listOf(Commit(patch.id, updates))
        )
    }

    private fun applyUpdates(state: String, updates: List<Update>): String =
            updates.fold(state) { acc, update -> applyUpdate(acc, update) }

    private fun applyUpdate(state: String, update: Update): String = when (update) {
        is AddLineUpdate -> ArrayList(state.split("\n"))
                .apply { add(update.lineIndex, "") }
                .joinToString("\n")

        is RemoveLineUpdate -> ArrayList(state.split("\n"))
                .apply { removeAt(update.lineIndex) }
                .joinToString("\n")

        is ChangeLineUpdate -> ArrayList(state.split("\n"))
                .apply {
                    this[update.lineIndex] = applyLineUpdate(this[update.lineIndex], update.update)
                }
                .joinToString("\n")
    }

    // visible for testing
    internal fun applyLineUpdate(line: String, lineUpdate: LineUpdate): String = when (lineUpdate) {
        is AddSymbols -> line.slice(0 until lineUpdate.index) + lineUpdate.symbols + line.slice(lineUpdate.index until line.length)
        is RemoveSymbols -> line.removeRange(lineUpdate.fromIndex..lineUpdate.toIndex)
    }


    private fun moveUpdate(patchUpdate: Update, documentUpdates: List<Update>): Update? =
            documentUpdates.fold(patchUpdate as Update?) { acc, next ->
                acc?.let { moveUpdate(acc, next) }
            }

    private fun merge(patchUpdates: List<Update>, documentUpdates: List<ChangeLineUpdate>): List<Update> =
            patchUpdates.mapNotNull {
                if (it is ChangeLineUpdate) documentUpdates
                        .fold(it as ChangeLineUpdate?) { acc, next ->
                            acc?.let { merge(acc, next) }
                        } else it
            }

    private fun merge(patchUpdate: ChangeLineUpdate, documentUpdate: ChangeLineUpdate): ChangeLineUpdate? {
        return when {
            documentUpdate.lineIndex == patchUpdate.lineIndex -> {
                val lineModification = mergeLineUpdate(patchUpdate.update, documentUpdate.update)

                lineModification?.let { patchUpdate.withUpdate(it) }

            }
            else -> patchUpdate
        }
    }

    private fun moveUpdate(patchUpdate: Update, documentUpdate: Update): Update? = when (documentUpdate) {
        is AddLineUpdate -> when {
            documentUpdate.lineIndex <= patchUpdate.lineIndex ->
                patchUpdate.withLineIndex(patchUpdate.lineIndex + 1)
            else -> patchUpdate
        }

        is RemoveLineUpdate -> when {
            documentUpdate.lineIndex < patchUpdate.lineIndex ->
                patchUpdate.withLineIndex(patchUpdate.lineIndex - 1)

            documentUpdate.lineIndex == patchUpdate.lineIndex -> when (patchUpdate) {
                is AddLineUpdate -> patchUpdate.withLineIndex(patchUpdate.lineIndex - 1)
                else -> null
            }

            else -> patchUpdate
        }

        else -> patchUpdate

    }

    // visible for testing
    internal fun mergeLineUpdate(
            patchUpdate: LineUpdate,
            docUpdate: LineUpdate
    ): LineUpdate? = when (docUpdate) {

        is AddSymbols -> {
            val shift = docUpdate.symbols.length
            when (patchUpdate) {
                is AddSymbols -> {
                    if (docUpdate.index <= patchUpdate.index) {
                        patchUpdate.copy(index = patchUpdate.index + shift)
                    } else patchUpdate
                }

                is RemoveSymbols -> when {
                    docUpdate.index <= patchUpdate.fromIndex -> patchUpdate.copy(
                            fromIndex = patchUpdate.fromIndex + shift,
                            toIndex = patchUpdate.toIndex + shift)
                    docUpdate.index < patchUpdate.toIndex ->
                        patchUpdate.copy(toIndex = patchUpdate.toIndex + shift)
                    else -> patchUpdate
                }
            }
        }

        is RemoveSymbols -> {
            when (patchUpdate) {
                is AddSymbols -> {
                    when {
                        patchUpdate.index in docUpdate.fromIndex until docUpdate.toIndex -> null
                        patchUpdate.index >= docUpdate.toIndex ->
                            patchUpdate.copy(index = patchUpdate.index - docUpdate.size())
                        else -> patchUpdate
                    }
                }

                is RemoveSymbols -> {
                    when {
                        // patch update is to the right of the document update
                        patchUpdate.fromIndex > docUpdate.toIndex ->
                            patchUpdate.copy(
                                    fromIndex = patchUpdate.fromIndex - docUpdate.size(),
                                    toIndex = patchUpdate.toIndex - docUpdate.size()
                            )

                        // patch update is to the left of the document update
                        patchUpdate.toIndex < docUpdate.fromIndex -> patchUpdate

                        // patch update is included in document update
                        docUpdate.fromIndex <= patchUpdate.fromIndex && docUpdate.toIndex >= patchUpdate.toIndex -> null

                        // patch update includes document update
                        patchUpdate.fromIndex <= docUpdate.fromIndex && patchUpdate.toIndex >= docUpdate.toIndex ->
                            patchUpdate.copy(toIndex = patchUpdate.toIndex - docUpdate.size())

                        // right part of patch update intersects document update
                        patchUpdate.fromIndex <= docUpdate.fromIndex && patchUpdate.toIndex >= docUpdate.fromIndex -> {
                            patchUpdate.copy(toIndex = patchUpdate.toIndex - (patchUpdate.toIndex - docUpdate.fromIndex + 1))
                        }

                        // left part of patch update intersects document update
                        docUpdate.fromIndex <= patchUpdate.fromIndex && patchUpdate.fromIndex <= docUpdate.toIndex -> {
                            val fromIndex = patchUpdate.fromIndex + (docUpdate.toIndex - patchUpdate.fromIndex + 1) - docUpdate.size()
                            val toIndex = patchUpdate.toIndex - docUpdate.size()
                            patchUpdate.copy(fromIndex = fromIndex, toIndex = toIndex)
                        }

                        else -> throw IllegalStateException("Unexpected condition for document update $docUpdate " +
                                "and patch update $patchUpdate")
                    }
                }
            }
        }
    }
}
