package studios.drible.tocabonito.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import studios.drible.tocabonito.feature.catalog.catalogGraph
import studios.drible.tocabonito.feature.detail.detailGraph
import studios.drible.tocabonito.feature.detail.detailRoute
import studios.drible.tocabonito.feature.downloads.downloadsGraph
import studios.drible.tocabonito.feature.mylist.myListGraph
import studios.drible.tocabonito.feature.settings.settingsGraph

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
        catalogGraph(
            onItemClick = { item ->
                navController.navigate(detailRoute(item.id, item.mediaType.value))
            },
        )
        detailGraph(
            onNavigateToPlayer = { _, _ -> /* player not implemented yet */ },
            onBack = { navController.popBackStack() },
        )
        downloadsGraph()
        myListGraph()
        settingsGraph(navController)
    }
}
