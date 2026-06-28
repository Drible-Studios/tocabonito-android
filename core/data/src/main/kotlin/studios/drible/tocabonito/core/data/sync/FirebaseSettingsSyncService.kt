package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSettingsSyncService @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val authWrapper: FirebaseAuthWrapper,
    private val localSettings: LocalSettingsReadWriter,
) : SettingsSyncService {

    private companion object {
        const val SETTINGS_COLLECTION = "users"
        const val SETTINGS_DOCUMENT = "settings"
    }

    private fun settingsPath(): String {
        val uid = authWrapper.currentUid ?: throw IllegalStateException("Not signed in")
        return "$SETTINGS_COLLECTION/$uid"
    }

    override suspend fun pushAll() {
        val allSettings = localSettings.readAll()
        firestoreClient.setDocument(
            path = settingsPath(),
            documentId = SETTINGS_DOCUMENT,
            data = allSettings,
        )
    }

    override suspend fun pullAll() {
        val remote = firestoreClient.getDocument(settingsPath(), SETTINGS_DOCUMENT) ?: return
        localSettings.applyAll(remote)
    }

    override suspend fun startObserving() = Unit

    override suspend fun stopObserving() = Unit
}
