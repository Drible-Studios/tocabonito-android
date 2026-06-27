package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubSyncStatusProvider @Inject constructor() : SyncStatusProvider {
    override fun observeStatus(): Flow<SyncStatus> = flowOf(SyncStatus.Disabled)
}
