package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFavoritesSyncService @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val authWrapper: FirebaseAuthWrapper,
) : FavoritesSyncService {

    private fun favoritesPath(): String {
        val uid = authWrapper.currentUid ?: throw IllegalStateException("Not signed in")
        return "users/$uid/favorites"
    }

    override suspend fun syncAdded(item: MediaItem) {
        firestoreClient.setDocument(
            path = favoritesPath(),
            documentId = item.id,
            data = item.toFavoriteDocument(),
        )
    }

    override suspend fun syncRemoved(id: String) {
        firestoreClient.deleteDocument(
            path = favoritesPath(),
            documentId = id,
        )
    }

    override suspend fun startObserving() = Unit

    override suspend fun stopObserving() = Unit
}
