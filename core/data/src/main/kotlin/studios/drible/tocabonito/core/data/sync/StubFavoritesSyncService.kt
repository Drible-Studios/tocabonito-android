package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubFavoritesSyncService @Inject constructor() : FavoritesSyncService {
    override suspend fun startObserving() = Unit
    override suspend fun stopObserving() = Unit
    override suspend fun syncAdded(item: MediaItem) = Unit
    override suspend fun syncRemoved(id: String) = Unit
}
