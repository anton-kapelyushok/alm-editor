package alm.editor.service

import alm.editor.DocumentId
import alm.editor.entity.DiffResponse
import alm.editor.entity.DocumentResponse
import alm.editor.entity.Patch
import alm.editor.repository.DocumentRepository
import org.springframework.stereotype.Service

@Service
class DocumentService(private val documentRepository: DocumentRepository,
                      private val patchService: PatchService) {

    fun createDocument(documentId: DocumentId) {
        documentRepository.createDocument(documentId)
    }

    fun getDocumentContent(documentId: DocumentId): DocumentResponse =
            documentRepository.findDocument(documentId).let {
                DocumentResponse(
                        revision = it.commits.size - 1,
                        content = it.state
                )
            }

    fun getDiff(documentId: DocumentId, clientRevision: Int): DiffResponse =
            documentRepository.findDocument(documentId).let {
                DiffResponse(
                        clientRevision = clientRevision,
                        serverRevision = it.commits.size - 1,
                        commits = it.commits.slice(clientRevision + 1 until it.commits.size)
                )
            }

    fun applyPatch(documentId: DocumentId, patch: Patch) =
            documentRepository.updateDocument(documentId) { document ->
                patchService.applyPatch(document, patch)
            }
}
