package alm.editor.entity

data class DocumentResponse(
        val revision: Int,
        val content: String
)

data class DiffResponse(
        val clientRevision: Int,
        val serverRevision: Int,
        val commits: List<Commit>
)
