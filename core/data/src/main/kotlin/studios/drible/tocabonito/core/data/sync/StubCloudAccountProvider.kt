package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubCloudAccountProvider @Inject constructor() : CloudAccountProvider {
    override suspend fun accountStatus(): CloudAccountStatus = CloudAccountStatus.NO_ACCOUNT
}
