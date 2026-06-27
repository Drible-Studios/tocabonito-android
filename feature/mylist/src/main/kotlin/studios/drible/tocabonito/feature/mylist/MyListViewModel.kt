package studios.drible.tocabonito.feature.mylist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import studios.drible.tocabonito.core.domain.repository.FavoritesRepository
import javax.inject.Inject

@HiltViewModel
class MyListViewModel @Inject constructor(
    favoritesRepository: FavoritesRepository,
) : ViewModel() {
    val state: StateFlow<MyListUiState> = favoritesRepository.observeAll()
        .map { items -> if (items.isEmpty()) MyListUiState.Empty else MyListUiState.Content(items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyListUiState.Empty)
}
