package studios.drible.tocabonito.core.data.sync

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class RealFirebaseAuthWrapper(
    private val firebaseAuth: FirebaseAuth,
) : FirebaseAuthWrapper {

    private val _authState = MutableStateFlow<AuthState>(firebaseAuth.currentUser.toAuthState())
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val listener = FirebaseAuth.AuthStateListener { auth ->
        _authState.value = auth.currentUser.toAuthState()
    }

    init {
        firebaseAuth.addAuthStateListener(listener)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}

private fun com.google.firebase.auth.FirebaseUser?.toAuthState(): AuthState =
    if (this != null) AuthState.SignedIn(uid, displayName)
    else AuthState.SignedOut
