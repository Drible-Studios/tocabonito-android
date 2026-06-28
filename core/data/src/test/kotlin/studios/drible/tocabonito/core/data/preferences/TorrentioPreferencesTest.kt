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

    private fun TestScope.createTestDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempDir, "test_torrentio_${System.nanoTime()}.preferences_pb") },
        )
    }

    @Test
    fun `providers initially emits defaults`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore())
        prefs.providers.test {
            awaitItem() shouldBe TorrentioPreferences.DEFAULT_PROVIDERS
        }
    }

    @Test
    fun `language initially emits default`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore())
        prefs.language.test {
            awaitItem() shouldBe "portuguese"
        }
    }

    @Test
    fun `saveProviders persists selection`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore())
        val selected = listOf("yts", "rarbg")
        prefs.saveProviders(selected)
        prefs.providers.test {
            awaitItem().toSet() shouldBe selected.toSet()
        }
    }

    @Test
    fun `saveLanguage persists selection`() = runTest {
        val prefs = TorrentioPreferences(createTestDataStore())
        prefs.saveLanguage("english")
        prefs.language.test {
            awaitItem() shouldBe "english"
        }
    }
}
