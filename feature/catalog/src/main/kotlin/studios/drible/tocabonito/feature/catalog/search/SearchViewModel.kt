package studios.drible.tocabonito.feature.catalog.search

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.repository.CatalogRepository
import studios.drible.tocabonito.core.ui.mvi.MviViewModel
import studios.drible.tocabonito.core.ui.util.toUserMessage
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : MviViewModel<SearchUiState, SearchIntent>(SearchUiState.Idle) {

    private val _query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    search(query)
                }
        }
    }

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> {
                val query = intent.query
                _query.value = query
                if (query.isEmpty()) {
                    setState { SearchUiState.Idle }
                } else {
                    setState { SearchUiState.Loading }
                }
            }
            is SearchIntent.Clear -> {
                _query.value = ""
                setState { SearchUiState.Idle }
            }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            try {
                val results = catalogRepository.search(query)
                setState { SearchUiState.Success(results = results, query = query) }
            } catch (e: Exception) {
                setState { SearchUiState.Error(e.toUserMessage()) }
            }
        }
    }
}
