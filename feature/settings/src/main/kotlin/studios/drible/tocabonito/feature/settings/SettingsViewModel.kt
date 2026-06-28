package studios.drible.tocabonito.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.data.api.realdebrid.RDUser
import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.preferences.ApiKeyStore
import studios.drible.tocabonito.core.data.preferences.TorrentioPreferences
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.DataPortabilityService
import studios.drible.tocabonito.core.domain.service.ImportResult
import studios.drible.tocabonito.core.domain.service.PortableBackup
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider
import studios.drible.tocabonito.core.ui.theme.ThemeProvider
import javax.inject.Inject

sealed class DataPortabilityState {
    data object Idle : DataPortabilityState()
    data object Loading : DataPortabilityState()
    data class ExportReady(val backup: PortableBackup) : DataPortabilityState()
    data class ImportDone(val result: ImportResult) : DataPortabilityState()
    data class Error(val message: String) : DataPortabilityState()
}

sealed class ApiValidationState {
    data object Idle : ApiValidationState()
    data object Loading : ApiValidationState()
    data class Success(val user: RDUser) : ApiValidationState()
    data class Error(val message: String) : ApiValidationState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val themeProvider: ThemeProvider,
    private val syncStatusProvider: SyncStatusProvider,
    private val cloudAccountProvider: CloudAccountProvider,
    private val dataPortabilityService: DataPortabilityService,
    private val apiKeyStore: ApiKeyStore,
    private val torrentioPreferences: TorrentioPreferences,
    private val realDebridClient: RealDebridClient,
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncStatusProvider.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus.Disabled)

    val isSignedIn: StateFlow<Boolean> = cloudAccountProvider.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val displayName: StateFlow<String?> = cloudAccountProvider.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun signOut() {
        viewModelScope.launch { cloudAccountProvider.signOut() }
    }

    private val _dataPortabilityState = MutableStateFlow<DataPortabilityState>(DataPortabilityState.Idle)
    val dataPortabilityState: StateFlow<DataPortabilityState> = _dataPortabilityState

    private val _apiValidationState = MutableStateFlow<ApiValidationState>(ApiValidationState.Idle)
    val apiValidationState: StateFlow<ApiValidationState> = _apiValidationState

    val storedApiKey: StateFlow<String?> = apiKeyStore.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val torrentioProviders: StateFlow<List<String>> = torrentioPreferences.providers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TorrentioPreferences.DEFAULT_PROVIDERS)

    val torrentioLanguage: StateFlow<String> = torrentioPreferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TorrentioPreferences.DEFAULT_LANGUAGE)

    fun validateApiKey(key: String) {
        viewModelScope.launch {
            _apiValidationState.update { ApiValidationState.Loading }
            runCatching { realDebridClient.user() }
                .onSuccess { user -> _apiValidationState.update { ApiValidationState.Success(user) } }
                .onFailure { t -> _apiValidationState.update { ApiValidationState.Error(t.message ?: "Validation failed") } }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch { apiKeyStore.save(key) }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            apiKeyStore.clear()
            _apiValidationState.update { ApiValidationState.Idle }
        }
    }

    fun updateProviders(list: List<String>) {
        viewModelScope.launch { torrentioPreferences.saveProviders(list) }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch { torrentioPreferences.saveLanguage(lang) }
    }

    fun exportData() {
        viewModelScope.launch {
            _dataPortabilityState.update { DataPortabilityState.Loading }
            runCatching { dataPortabilityService.exportAll() }
                .onSuccess { backup -> _dataPortabilityState.update { DataPortabilityState.ExportReady(backup) } }
                .onFailure { t -> _dataPortabilityState.update { DataPortabilityState.Error(t.message ?: "Export failed") } }
        }
    }

    fun importData(backup: PortableBackup) {
        viewModelScope.launch {
            _dataPortabilityState.update { DataPortabilityState.Loading }
            runCatching { dataPortabilityService.importAll(backup) }
                .onSuccess { result -> _dataPortabilityState.update { DataPortabilityState.ImportDone(result) } }
                .onFailure { t -> _dataPortabilityState.update { DataPortabilityState.Error(t.message ?: "Import failed") } }
        }
    }

    fun clearPortabilityState() {
        _dataPortabilityState.update { DataPortabilityState.Idle }
    }
}
