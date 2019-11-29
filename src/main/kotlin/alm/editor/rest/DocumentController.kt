package alm.editor.rest

import alm.editor.DocumentId
import alm.editor.entity.DocumentResponse
import alm.editor.entity.Patch
import alm.editor.service.DocumentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/documents/{documentId}")
class DocumentController(
        private val documentService: DocumentService
) {

    @PutMapping
    fun createDocument(@PathVariable documentId: DocumentId): Unit = documentService.createDocument(documentId)

    @PostMapping
    fun applyPatch(@PathVariable documentId: DocumentId, @RequestBody patch: Patch) {
        documentService.applyPatch(documentId, patch)
    }

    @GetMapping("/content")
    fun getDocumentContent(@PathVariable documentId: DocumentId): DocumentResponse =
            documentService.getDocumentContent(documentId)

    @GetMapping("/diff/{revisionId}")
    fun getDocumentDiff(@PathVariable documentId: DocumentId, @PathVariable revisionId: Int) =
            documentService.getDiff(documentId, revisionId)
}
