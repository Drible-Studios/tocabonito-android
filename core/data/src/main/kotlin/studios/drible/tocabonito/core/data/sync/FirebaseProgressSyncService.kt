package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProgressSyncService @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val authWrapper: FirebaseAuthWrapper,
) : ProgressSyncService {

    private fun progressPath(): String {
        val uid = authWrapper.currentUid ?: throw IllegalStateException("Not signed in")
        return "users/$uid/progress"
    }

    override suspend fun syncProgress(progress: WatchProgress) {
        val path = progressPath()
        val documentId = progress.id
        val existing = firestoreClient.getDocument(path, documentId)
        val remoteLastWatched = (existing?.get("lastWatched") as? Number)?.toLong() ?: 0L

        if (progress.lastWatched >= remoteLastWatched) {
            firestoreClient.setDocument(
                path = path,
                documentId = documentId,
                data = progress.toProgressDocument(),
            )
        }
    }

    override suspend fun startObserving() = Unit

    override suspend fun stopObserving() = Unit
}
