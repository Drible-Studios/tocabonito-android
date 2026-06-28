package studios.drible.tocabonito.core.data.sync

class FakeFirestoreClient : FirestoreClient {
    val documents = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()
    val batchOperations = mutableListOf<BatchOperation>()

    override suspend fun setDocument(path: String, documentId: String, data: Map<String, Any?>) {
        documents.getOrPut(path) { mutableMapOf() }[documentId] = data
    }

    override suspend fun deleteDocument(path: String, documentId: String) {
        documents[path]?.remove(documentId)
    }

    override suspend fun getDocuments(path: String): List<Map<String, Any?>> {
        return documents[path]?.values?.toList() ?: emptyList()
    }

    override suspend fun getDocument(path: String, documentId: String): Map<String, Any?>? {
        return documents[path]?.get(documentId)
    }

    override suspend fun batchWrite(operations: List<BatchOperation>) {
        batchOperations.addAll(operations)
        for (op in operations) {
            when (op) {
                is BatchOperation.Set -> setDocument(op.path, op.documentId, op.data)
                is BatchOperation.Delete -> deleteDocument(op.path, op.documentId)
            }
        }
    }
}
