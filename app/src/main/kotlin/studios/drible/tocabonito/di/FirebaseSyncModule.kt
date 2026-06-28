package studios.drible.tocabonito.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.data.sync.FirebaseAccountProvider
import studios.drible.tocabonito.core.data.sync.FirebaseFavoritesSyncService
import studios.drible.tocabonito.core.data.sync.FirebaseProgressSyncService
import studios.drible.tocabonito.core.data.sync.FirebaseSettingsSyncService
import studios.drible.tocabonito.core.data.sync.FirebaseAuthWrapper
import studios.drible.tocabonito.core.data.sync.FirestoreClient
import studios.drible.tocabonito.core.data.sync.LocalSettingsReadWriter
import studios.drible.tocabonito.core.data.sync.RealFirebaseAuthWrapper
import studios.drible.tocabonito.core.data.sync.RealFirestoreClient
import studios.drible.tocabonito.core.data.sync.StubLocalSettingsReadWriter
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseSyncProvidesModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseSyncBindsModule {

    @Binds
    abstract fun bindFirebaseAuthWrapper(impl: RealFirebaseAuthWrapper): FirebaseAuthWrapper

    @Binds
    abstract fun bindFirestoreClient(impl: RealFirestoreClient): FirestoreClient

    @Binds
    abstract fun bindCloudAccountProvider(impl: FirebaseAccountProvider): CloudAccountProvider

    @Binds
    abstract fun bindFavoritesSyncService(impl: FirebaseFavoritesSyncService): FavoritesSyncService

    @Binds
    abstract fun bindProgressSyncService(impl: FirebaseProgressSyncService): ProgressSyncService

    @Binds
    abstract fun bindSettingsSyncService(impl: FirebaseSettingsSyncService): SettingsSyncService

    @Binds
    abstract fun bindLocalSettingsReadWriter(impl: StubLocalSettingsReadWriter): LocalSettingsReadWriter
}
