package studios.drible.tocabonito.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE = "settings"
const val THEME_PICKER_ROUTE = "settings/theme"

fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onSignInClick: () -> Unit = {},
) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(
            onNavigateToThemePicker = { navController.navigate(THEME_PICKER_ROUTE) },
            onSignInClick = onSignInClick,
        )
    }
    composable(THEME_PICKER_ROUTE) {
        ThemePickerScreen(onBack = { navController.popBackStack() })
    }
}
