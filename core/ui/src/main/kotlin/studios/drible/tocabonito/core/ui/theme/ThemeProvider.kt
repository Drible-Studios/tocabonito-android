package studios.drible.tocabonito.core.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ThemeStore {
    fun load(): String?
    fun save(value: String)
}

@Singleton
class ThemeProvider @Inject constructor(
    private val store: ThemeStore,
) {
    private val _selectedTheme = MutableStateFlow(loadInitial())

    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    val currentPalette: ThemePalette
        get() = _selectedTheme.value.palette

    fun selectTheme(theme: AppTheme) {
        _selectedTheme.value = theme
        store.save(theme.name)
    }

    private fun loadInitial(): AppTheme {
        val stored = store.load() ?: return AppTheme.CANARINHO
        return AppTheme.entries.find { it.name == stored } ?: AppTheme.CANARINHO
    }
}
