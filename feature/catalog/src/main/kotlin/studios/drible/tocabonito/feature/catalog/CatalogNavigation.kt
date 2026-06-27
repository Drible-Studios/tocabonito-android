package studios.drible.tocabonito.feature.catalog

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.feature.catalog.explore.ExploreScreen
import studios.drible.tocabonito.feature.catalog.home.HomeScreen
import studios.drible.tocabonito.feature.catalog.search.SearchScreen

const val HOME_ROUTE = "home"
const val EXPLORE_ROUTE = "explore"
const val SEARCH_ROUTE = "search"

fun NavGraphBuilder.catalogGraph(onItemClick: (MediaItem) -> Unit) {
    composable(HOME_ROUTE) { HomeScreen(onItemClick = onItemClick) }
    composable(EXPLORE_ROUTE) { ExploreScreen(onItemClick = onItemClick) }
    composable(SEARCH_ROUTE) { SearchScreen(onItemClick = onItemClick) }
}
