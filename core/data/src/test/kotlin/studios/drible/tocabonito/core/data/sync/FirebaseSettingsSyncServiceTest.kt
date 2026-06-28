package studios.drible.tocabonito.core.data.sync

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FirebaseSettingsSyncServiceTest : FunSpec({

    test("pushAll uploads local settings to Firestore") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        val localSettings = FakeLocalSettingsReadWriter()
        localSettings.settings["theme"] = "dark"
        localSettings.settings["language"] = "en"

        val service = FirebaseSettingsSyncService(client, auth, localSettings)
        service.pushAll()

        val path = "users/user1"
        val doc = client.documents[path]!!["settings"]!!
        doc["theme"] shouldBe "dark"
        doc["language"] shouldBe "en"
    }

    test("pullAll applies remote settings to local") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        client.setDocument("users/user1", "settings", mapOf("theme" to "light", "autoplay" to true))
        val localSettings = FakeLocalSettingsReadWriter()

        val service = FirebaseSettingsSyncService(client, auth, localSettings)
        service.pullAll()

        localSettings.settings["theme"] shouldBe "light"
        localSettings.settings["autoplay"] shouldBe true
    }

    test("pullAll does nothing when no remote settings exist") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        val localSettings = FakeLocalSettingsReadWriter()
        localSettings.settings["existing"] = "value"

        val service = FirebaseSettingsSyncService(client, auth, localSettings)
        service.pullAll()

        localSettings.settings["existing"] shouldBe "value"
        localSettings.settings.size shouldBe 1
    }

    test("pushAll uses correct user-specific path") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "custom-uid")
        val client = FakeFirestoreClient()
        val localSettings = FakeLocalSettingsReadWriter()
        localSettings.settings["key"] = "value"

        val service = FirebaseSettingsSyncService(client, auth, localSettings)
        service.pushAll()

        client.documents.containsKey("users/custom-uid") shouldBe true
    }
})
