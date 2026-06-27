package studios.drible.tocabonito.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider

class FakeSettingsSyncService : SettingsSyncService {
    override suspend fun pushAll() {
        // No-op
    }

    override suspend fun pullAll() {
        // No-op
    }

    override suspend fun startObserving() {
        // No-op
    }

    override suspend fun stopObserving() {
        // No-op
    }
}

class FakeFavoritesSyncService : FavoritesSyncService {
    override suspend fun startObserving() {
        // No-op
    }

    override suspend fun stopObserving() {
        // No-op
    }

    override suspend fun syncAdded(item: MediaItem) {
        // No-op
    }

    override suspend fun syncRemoved(id: String) {
        // No-op
    }
}

class FakeProgressSyncService : ProgressSyncService {
    override suspend fun startObserving() {
        // No-op
    }

    override suspend fun stopObserving() {
        // No-op
    }

    override suspend fun syncProgress(progress: WatchProgress) {
        // No-op
    }
}

class FakeCloudAccountProvider : CloudAccountProvider {
    var accountStatus: CloudAccountStatus = CloudAccountStatus.AVAILABLE

    override suspend fun accountStatus(): CloudAccountStatus = this.accountStatus
}

class FakeSyncStatusProvider : SyncStatusProvider {
    var status: SyncStatus = SyncStatus.Disabled

    override fun observeStatus(): Flow<SyncStatus> = flowOf(status)
}
