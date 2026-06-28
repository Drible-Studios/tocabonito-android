package studios.drible.tocabonito.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ApiKeyStoreTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTestDataStore(testScope: TestScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempDir, "test_api_keys.preferences_pb") },
        )
    }

    @Test
    fun `apiKey initially emits null`() = runTest {
        val store = ApiKeyStore(createTestDataStore(this))
        store.apiKey.test {
            awaitItem().shouldBeNull()
        }
    }

    @Test
    fun `save persists key`() = runTest {
        val store = ApiKeyStore(createTestDataStore(this))
        store.save("my-secret-key")
        store.apiKey.test {
            awaitItem() shouldBe "my-secret-key"
        }
    }

    @Test
    fun `clear removes key`() = runTest {
        val store = ApiKeyStore(createTestDataStore(this))
        store.save("my-secret-key")
        store.clear()
        store.apiKey.test {
            awaitItem().shouldBeNull()
        }
    }

    @Test
    fun `effectiveKey returns stored key when present`() = runTest {
        val store = ApiKeyStore(createTestDataStore(this))
        store.save("stored-key")
        store.effectiveKey() shouldBe "stored-key"
    }
}
