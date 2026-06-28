package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFirebaseAuthWrapper : FirebaseAuthWrapper {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signOut() {
        _authState.value = AuthState.SignedOut
    }

    fun simulateSignIn(uid: String = "test-uid", displayName: String? = "Test User") {
        _authState.value = AuthState.SignedIn(uid, displayName)
    }

    fun simulateSignOut() {
        _authState.value = AuthState.SignedOut
    }
}
