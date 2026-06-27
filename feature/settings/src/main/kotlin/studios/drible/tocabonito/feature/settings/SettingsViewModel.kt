package studios.drible.tocabonito.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import studios.drible.tocabonito.core.ui.theme.ThemeProvider
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val themeProvider: ThemeProvider,
) : ViewModel()
