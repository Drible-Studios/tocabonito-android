package studios.drible.tocabonito.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.data.sync.DataPortabilityServiceImpl
import studios.drible.tocabonito.core.data.sync.StubCloudAccountProvider
import studios.drible.tocabonito.core.data.sync.StubFavoritesSyncService
import studios.drible.tocabonito.core.data.sync.StubProgressSyncService
import studios.drible.tocabonito.core.data.sync.StubSettingsSyncService
import studios.drible.tocabonito.core.data.sync.StubSyncStatusProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.DataPortabilityService
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindCloudAccountProvider(impl: StubCloudAccountProvider): CloudAccountProvider

    @Binds
    abstract fun bindSyncStatusProvider(impl: StubSyncStatusProvider): SyncStatusProvider

    @Binds
    abstract fun bindProgressSyncService(impl: StubProgressSyncService): ProgressSyncService

    @Binds
    abstract fun bindFavoritesSyncService(impl: StubFavoritesSyncService): FavoritesSyncService

    @Binds
    abstract fun bindSettingsSyncService(impl: StubSettingsSyncService): SettingsSyncService

    @Binds
    abstract fun bindDataPortabilityService(impl: DataPortabilityServiceImpl): DataPortabilityService
}
