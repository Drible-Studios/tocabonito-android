package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAccountProvider @Inject constructor(
    private val authWrapper: FirebaseAuthWrapper,
) : CloudAccountProvider {

    override val isSignedIn: Flow<Boolean> =
        authWrapper.authState.map { it is AuthState.SignedIn }

    override val displayName: Flow<String?> =
        authWrapper.authState.map { (it as? AuthState.SignedIn)?.displayName }

    override val currentUid: String?
        get() = authWrapper.currentUid

    override suspend fun accountStatus(): CloudAccountStatus =
        when (authWrapper.authState.value) {
            is AuthState.SignedIn -> CloudAccountStatus.AVAILABLE
            is AuthState.SignedOut -> CloudAccountStatus.NO_ACCOUNT
        }

    override suspend fun signOut() {
        authWrapper.signOut()
    }
}
