package studios.drible.tocabonito.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentioPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val providersPref = stringSetPreferencesKey("torrentio_providers")
    private val languagePref = stringPreferencesKey("torrentio_language")

    val providers: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[providersPref]?.toList() ?: DEFAULT_PROVIDERS
    }

    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[languagePref] ?: DEFAULT_LANGUAGE
    }

    suspend fun saveProviders(list: List<String>) {
        dataStore.edit { prefs -> prefs[providersPref] = list.toSet() }
    }

    suspend fun saveLanguage(lang: String) {
        dataStore.edit { prefs -> prefs[languagePref] = lang }
    }

    companion object {
        val DEFAULT_PROVIDERS = listOf(
            "yts", "eztv", "rarbg", "1337x",
            "thepiratebay", "kickasstorrents", "torrentgalaxy",
        )

        const val DEFAULT_LANGUAGE = "portuguese"

        val AVAILABLE_LANGUAGES = listOf(
            "portuguese", "english", "spanish", "french", "german",
            "italian", "dutch", "russian", "japanese", "korean",
            "chinese", "arabic", "hindi", "turkish", "polish",
        )
    }
}
