package studios.drible.tocabonito.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TorrentioPreferencesTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTestDataStore(testScope: TestScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempDir, "test_torrentio.preferences_pb") },
        )
    }

    @Test
    fun `providers initially emits defaults`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore(this))
        prefs.providers.test {
            awaitItem() shouldBe TorrentioPreferences.DEFAULT_PROVIDERS
        }
    }

    @Test
    fun `language initially emits default`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore(this))
        prefs.language.test {
            awaitItem() shouldBe "portuguese"
        }
    }

    @Test
    fun `saveProviders persists selection`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore(this))
        val selected = listOf("yts", "rarbg")
        prefs.saveProviders(selected)
        prefs.providers.test {
            awaitItem().toSet() shouldBe selected.toSet()
        }
    }

    @Test
    fun `saveLanguage persists selection`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore(this))
        prefs.saveLanguage("english")
        prefs.language.test {
            awaitItem() shouldBe "english"
        }
    }
}
