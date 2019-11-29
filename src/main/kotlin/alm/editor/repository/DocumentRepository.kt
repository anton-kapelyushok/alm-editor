package alm.editor.repository

import alm.editor.DocumentId
import alm.editor.entity.Document
import alm.editor.entity.emptyDocument
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class DocumentRepository {
    private val lockRegistry = ConcurrentHashMap<DocumentId, Lock>()
    private val documentRegistry = mutableMapOf<DocumentId, Document>()

    fun findDocument(documentId: DocumentId): Document = documentRegistry.getValue(documentId)

    fun createDocument(documentId: DocumentId): Document {
        val lock = ReentrantLock()
        lock.withLock {
            if (lockRegistry.putIfAbsent(documentId, lock) != null) {
                throw IllegalStateException("Document $documentId already exists")
            }
            val document = emptyDocument(documentId)
            documentRegistry[documentId] = document
            return document
        }
    }

    fun updateDocument(documentId: DocumentId, update: (Document) -> Document): Document {
        return lockRegistry.getValue(documentId).withLock {
            val document = documentRegistry.getValue(documentId)
            val updated = update(document)
            documentRegistry[documentId] = updated
            updated
        }
    }
}
