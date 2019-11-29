package alm.editor.entity

import alm.editor.DocumentId
import alm.editor.PatchId
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

fun emptyDocument(id: DocumentId) = Document(id = id, commits = listOf(Commit("initial")), state = "")

data class Document(
        val id: DocumentId,
        val commits: List<Commit>,
        val state: String
)

data class Commit(
        val patchId: PatchId,
        val updates: List<Update> = emptyList()
)

data class Patch(
        val id: PatchId,
        val revision: Int,
        val updates: List<Update>
)

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = AddLineUpdate::class, name = "add-line"),
        JsonSubTypes.Type(value = RemoveLineUpdate::class, name = "remove-line"),
        JsonSubTypes.Type(value = ChangeLineUpdate::class, name = "change-line"))
sealed class Update(
        val lineIndex: Int
) {
    abstract fun withLineIndex(newIndex: Int): Update
}


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = AddSymbols::class, name = "add-symbols"),
        JsonSubTypes.Type(value = RemoveSymbols::class, name = "remove-symbols"))
sealed class LineUpdate

class AddLineUpdate(
        lineIndex: Int
) : Update(lineIndex) {
    override fun withLineIndex(newIndex: Int) = AddLineUpdate(lineIndex)
}

class RemoveLineUpdate(
        lineIndex: Int
) : Update(lineIndex) {
    override fun withLineIndex(newIndex: Int) = RemoveLineUpdate(newIndex)
}

class ChangeLineUpdate(
        lineIndex: Int,
        val update: LineUpdate
) : Update(lineIndex) {
    override fun withLineIndex(newIndex: Int) = ChangeLineUpdate(newIndex, update)

    fun withUpdate(newUpdate: LineUpdate) = ChangeLineUpdate(lineIndex, newUpdate)
}

data class AddSymbols(
        val index: Int,
        val symbols: String
) : LineUpdate()

data class RemoveSymbols(
        val fromIndex: Int,
        val toIndex: Int
) : LineUpdate() {
    fun size() = toIndex - fromIndex + 1
}
