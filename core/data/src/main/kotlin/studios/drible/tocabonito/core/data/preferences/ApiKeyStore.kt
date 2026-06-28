package studios.drible.tocabonito.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.data.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiKeyDataStore: DataStore<Preferences> by preferencesDataStore(name = "api_keys")

@Singleton
class ApiKeyStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val keyPref = stringPreferencesKey("rd_api_key")

    val apiKey: Flow<String?> = dataStore.data.map { prefs -> prefs[keyPref] }

    suspend fun save(key: String) {
        dataStore.edit { prefs -> prefs[keyPref] = key }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(keyPref) }
    }

    suspend fun effectiveKey(): String? {
        val stored = dataStore.data.first()[keyPref]
        if (!stored.isNullOrBlank()) return stored
        val fallback = BuildConfig.RD_API_KEY
        return fallback.ifBlank { null }
    }
}
