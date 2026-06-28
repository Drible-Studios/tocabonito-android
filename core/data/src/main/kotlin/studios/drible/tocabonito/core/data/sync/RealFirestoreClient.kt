package studios.drible.tocabonito.core.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealFirestoreClient @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FirestoreClient {

    override suspend fun setDocument(path: String, documentId: String, data: Map<String, Any?>) {
        firestore.collection(path).document(documentId).set(data).await()
    }

    override suspend fun deleteDocument(path: String, documentId: String) {
        firestore.collection(path).document(documentId).delete().await()
    }

    override suspend fun getDocuments(path: String): List<Map<String, Any?>> {
        val snapshot = firestore.collection(path).get().await()
        return snapshot.documents.mapNotNull { it.data }
    }

    override suspend fun getDocument(path: String, documentId: String): Map<String, Any?>? {
        val snapshot = firestore.collection(path).document(documentId).get().await()
        return snapshot.data
    }

    override suspend fun batchWrite(operations: List<BatchOperation>) {
        val batch = firestore.batch()
        for (op in operations) {
            when (op) {
                is BatchOperation.Set -> {
                    val ref = firestore.collection(op.path).document(op.documentId)
                    batch.set(ref, op.data)
                }
                is BatchOperation.Delete -> {
                    val ref = firestore.collection(op.path).document(op.documentId)
                    batch.delete(ref)
                }
            }
        }
        batch.commit().await()
    }
}
