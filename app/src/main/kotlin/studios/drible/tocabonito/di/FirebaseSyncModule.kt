package studios.drible.tocabonito.di

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import studios.drible.tocabonito.core.data.sync.FirebaseAccountProvider
import studios.drible.tocabonito.core.data.sync.FirebaseAuthWrapper
import studios.drible.tocabonito.core.data.sync.FirebaseFavoritesSyncService
import studios.drible.tocabonito.core.data.sync.FirebaseProgressSyncService
import studios.drible.tocabonito.core.data.sync.FirebaseSettingsSyncService
import studios.drible.tocabonito.core.data.sync.FirestoreClient
import studios.drible.tocabonito.core.data.sync.LocalSettingsReadWriter
import studios.drible.tocabonito.core.data.sync.RealFirebaseAuthWrapper
import studios.drible.tocabonito.core.data.sync.RealFirestoreClient
import studios.drible.tocabonito.core.data.sync.StubCloudAccountProvider
import studios.drible.tocabonito.core.data.sync.StubFavoritesSyncService
import studios.drible.tocabonito.core.data.sync.StubLocalSettingsReadWriter
import studios.drible.tocabonito.core.data.sync.StubProgressSyncService
import studios.drible.tocabonito.core.data.sync.StubSettingsSyncService
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseSyncModule {

    private fun isFirebaseAvailable(@ApplicationContext context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty()

    @Provides
    @Singleton
    fun provideCloudAccountProvider(
        @ApplicationContext context: Context,
    ): CloudAccountProvider {
        if (!isFirebaseAvailable(context)) return StubCloudAccountProvider()
        val auth = FirebaseAuth.getInstance()
        val wrapper = RealFirebaseAuthWrapper(auth)
        return FirebaseAccountProvider(wrapper)
    }

    @Provides
    @Singleton
    fun provideFavoritesSyncService(
        @ApplicationContext context: Context,
    ): FavoritesSyncService {
        if (!isFirebaseAvailable(context)) return StubFavoritesSyncService()
        val firestore = FirebaseFirestore.getInstance()
        val client = RealFirestoreClient(firestore)
        val authWrapper = RealFirebaseAuthWrapper(FirebaseAuth.getInstance())
        return FirebaseFavoritesSyncService(client, authWrapper)
    }

    @Provides
    @Singleton
    fun provideProgressSyncService(
        @ApplicationContext context: Context,
    ): ProgressSyncService {
        if (!isFirebaseAvailable(context)) return StubProgressSyncService()
        val firestore = FirebaseFirestore.getInstance()
        val client = RealFirestoreClient(firestore)
        val authWrapper = RealFirebaseAuthWrapper(FirebaseAuth.getInstance())
        return FirebaseProgressSyncService(client, authWrapper)
    }

    @Provides
    @Singleton
    fun provideSettingsSyncService(
        @ApplicationContext context: Context,
    ): SettingsSyncService {
        if (!isFirebaseAvailable(context)) return StubSettingsSyncService()
        val firestore = FirebaseFirestore.getInstance()
        val client = RealFirestoreClient(firestore)
        val authWrapper = RealFirebaseAuthWrapper(FirebaseAuth.getInstance())
        val localSettings = StubLocalSettingsReadWriter()
        return FirebaseSettingsSyncService(client, authWrapper, localSettings)
    }

    @Provides
    @Singleton
    fun provideLocalSettingsReadWriter(): LocalSettingsReadWriter = StubLocalSettingsReadWriter()
}
