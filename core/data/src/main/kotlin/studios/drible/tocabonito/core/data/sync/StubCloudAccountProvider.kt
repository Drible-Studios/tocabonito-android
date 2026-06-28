package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubCloudAccountProvider @Inject constructor() : CloudAccountProvider {
    override suspend fun accountStatus(): CloudAccountStatus = CloudAccountStatus.NO_ACCOUNT
    override val isSignedIn: Flow<Boolean> = flowOf(false)
    override val displayName: Flow<String?> = flowOf(null)
    override val currentUid: String? = null
    override suspend fun signOut() {}
}
