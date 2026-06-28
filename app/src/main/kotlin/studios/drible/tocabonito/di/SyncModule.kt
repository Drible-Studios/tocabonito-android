package studios.drible.tocabonito.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.data.sync.DataPortabilityServiceImpl
import studios.drible.tocabonito.core.data.sync.StubSyncStatusProvider
import studios.drible.tocabonito.core.domain.service.DataPortabilityService
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindSyncStatusProvider(impl: StubSyncStatusProvider): SyncStatusProvider

    @Binds
    abstract fun bindDataPortabilityService(impl: DataPortabilityServiceImpl): DataPortabilityService
}
