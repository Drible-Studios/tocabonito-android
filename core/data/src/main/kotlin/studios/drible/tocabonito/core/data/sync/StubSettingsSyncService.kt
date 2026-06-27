package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubSettingsSyncService @Inject constructor() : SettingsSyncService {
    override suspend fun pushAll() = Unit
    override suspend fun pullAll() = Unit
    override suspend fun startObserving() = Unit
    override suspend fun stopObserving() = Unit
}
