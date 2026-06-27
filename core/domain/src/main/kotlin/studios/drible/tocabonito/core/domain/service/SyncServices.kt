package studios.drible.tocabonito.core.domain.service

import kotlinx.coroutines.flow.Flow
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.WatchProgress

interface SettingsSyncService {
    suspend fun pushAll()
    suspend fun pullAll()
    suspend fun startObserving()
    suspend fun stopObserving()
}

interface FavoritesSyncService {
    suspend fun startObserving()
    suspend fun stopObserving()
    suspend fun syncAdded(item: MediaItem)
    suspend fun syncRemoved(id: String)
}

interface ProgressSyncService {
    suspend fun startObserving()
    suspend fun stopObserving()
    suspend fun syncProgress(progress: WatchProgress)
}

enum class CloudAccountStatus {
    AVAILABLE,
    NO_ACCOUNT,
    RESTRICTED,
    COULD_NOT_DETERMINE,
    TEMPORARILY_UNAVAILABLE,
}

interface CloudAccountProvider {
    suspend fun accountStatus(): CloudAccountStatus
}

enum class SyncError {
    QUOTA_EXCEEDED,
    NETWORK_UNAVAILABLE,
    ACCOUNT_CHANGED,
    UNKNOWN,
}

sealed class SyncStatus {
    data object Disabled : SyncStatus()
    data class Idle(val lastSynced: Long?) : SyncStatus()
    data object Syncing : SyncStatus()
    data class Error(val error: SyncError) : SyncStatus()
    data object AccountUnavailable : SyncStatus()
}

interface SyncStatusProvider {
    fun observeStatus(): Flow<SyncStatus>
}
