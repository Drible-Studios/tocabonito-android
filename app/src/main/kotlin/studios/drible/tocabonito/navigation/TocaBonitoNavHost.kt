package studios.drible.tocabonito.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

@Composable
fun TocaBonitoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            composable(destination.route) {
                PlaceholderScreen(label = destination.label)
            }
        }
        composable("settings") {
            PlaceholderScreen(label = "Settings")
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    val palette = LocalThemePalette.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Coming soon — $label",
            color = palette.textPrimary,
        )
    }
}
