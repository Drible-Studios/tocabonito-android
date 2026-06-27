package studios.drible.tocabonito.core.ui.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class MviViewModel<State, Intent>(initialState: State) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    protected val currentState: State get() = _state.value

    protected fun setState(reducer: State.() -> State) {
        _state.value = _state.value.reducer()
    }

    abstract fun onIntent(intent: Intent)
}
