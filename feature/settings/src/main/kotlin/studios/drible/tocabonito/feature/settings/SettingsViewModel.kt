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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val themeProvider: ThemeProvider,
    private val syncStatusProvider: SyncStatusProvider,
    private val cloudAccountProvider: CloudAccountProvider,
    private val dataPortabilityService: DataPortabilityService,
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncStatusProvider.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus.Disabled)

    private val _dataPortabilityState = MutableStateFlow<DataPortabilityState>(DataPortabilityState.Idle)
    val dataPortabilityState: StateFlow<DataPortabilityState> = _dataPortabilityState

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
