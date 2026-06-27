package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubProgressSyncService @Inject constructor() : ProgressSyncService {
    override suspend fun startObserving() = Unit
    override suspend fun stopObserving() = Unit
    override suspend fun syncProgress(progress: WatchProgress) = Unit
}
