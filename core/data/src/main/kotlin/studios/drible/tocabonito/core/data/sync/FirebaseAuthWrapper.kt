package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    data class SignedIn(val uid: String, val displayName: String?) : AuthState()
    data object SignedOut : AuthState()
}

interface FirebaseAuthWrapper {
    val authState: StateFlow<AuthState>
    val currentUid: String? get() = (authState.value as? AuthState.SignedIn)?.uid
    suspend fun signOut()
}
