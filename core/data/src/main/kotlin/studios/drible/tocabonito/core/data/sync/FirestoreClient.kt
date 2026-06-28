package studios.drible.tocabonito.core.data.sync

sealed class BatchOperation {
    data class Set(val path: String, val documentId: String, val data: Map<String, Any?>) : BatchOperation()
    data class Delete(val path: String, val documentId: String) : BatchOperation()
}

interface FirestoreClient {
    suspend fun setDocument(path: String, documentId: String, data: Map<String, Any?>)
    suspend fun deleteDocument(path: String, documentId: String)
    suspend fun getDocuments(path: String): List<Map<String, Any?>>
    suspend fun getDocument(path: String, documentId: String): Map<String, Any?>?
    suspend fun batchWrite(operations: List<BatchOperation>)
}
