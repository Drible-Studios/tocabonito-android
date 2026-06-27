package studios.drible.tocabonito.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val palette = LocalThemePalette.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            if (currentRoute == TopLevelDestination.HOME.route) {
                TopAppBar(
                    title = { Text("Toca Bonito", color = palette.textPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.background,
                    ),
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = palette.textPrimary,
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = palette.cardBackground) {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.accent,
                            selectedTextColor = palette.accent,
                            unselectedIconColor = palette.textSecondary,
                            unselectedTextColor = palette.textSecondary,
                            indicatorColor = palette.surfaceElevated,
                        ),
                    )
                }
            }
        },
        containerColor = palette.background,
    ) { innerPadding ->
        TocaBonitoNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .background(palette.background),
        )
    }
}
