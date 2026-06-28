package studios.drible.tocabonito.core.data.sync

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus

class FirebaseAccountProviderTest : FunSpec({

    test("isSignedIn emits false when signed out") {
        val auth = FakeFirebaseAuthWrapper()
        val provider = FirebaseAccountProvider(auth)

        provider.isSignedIn.test {
            awaitItem() shouldBe false
            cancelAndConsumeRemainingEvents()
        }
    }

    test("isSignedIn emits true when signed in") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn()
        val provider = FirebaseAccountProvider(auth)

        provider.isSignedIn.test {
            awaitItem() shouldBe true
            cancelAndConsumeRemainingEvents()
        }
    }

    test("displayName emits null when signed out") {
        val auth = FakeFirebaseAuthWrapper()
        val provider = FirebaseAccountProvider(auth)

        provider.displayName.test {
            awaitItem() shouldBe null
            cancelAndConsumeRemainingEvents()
        }
    }

    test("displayName emits name when signed in") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(displayName = "Alice")
        val provider = FirebaseAccountProvider(auth)

        provider.displayName.test {
            awaitItem() shouldBe "Alice"
            cancelAndConsumeRemainingEvents()
        }
    }

    test("currentUid returns null when signed out") {
        val auth = FakeFirebaseAuthWrapper()
        val provider = FirebaseAccountProvider(auth)

        provider.currentUid shouldBe null
    }

    test("currentUid returns uid when signed in") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "uid-123")
        val provider = FirebaseAccountProvider(auth)

        provider.currentUid shouldBe "uid-123"
    }

    test("accountStatus returns NO_ACCOUNT when signed out") {
        val auth = FakeFirebaseAuthWrapper()
        val provider = FirebaseAccountProvider(auth)

        provider.accountStatus() shouldBe CloudAccountStatus.NO_ACCOUNT
    }

    test("accountStatus returns AVAILABLE when signed in") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn()
        val provider = FirebaseAccountProvider(auth)

        provider.accountStatus() shouldBe CloudAccountStatus.AVAILABLE
    }

    test("signOut delegates to auth wrapper") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn()
        val provider = FirebaseAccountProvider(auth)

        provider.signOut()

        auth.authState.value shouldBe AuthState.SignedOut
    }
})
