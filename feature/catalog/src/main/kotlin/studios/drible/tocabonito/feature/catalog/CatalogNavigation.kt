package studios.drible.tocabonito.feature.catalog

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val HOME_ROUTE = "home"
const val EXPLORE_ROUTE = "explore"
const val SEARCH_ROUTE = "search"

fun NavGraphBuilder.catalogGraph() {
    composable(HOME_ROUTE) { /* placeholder */ }
    composable(EXPLORE_ROUTE) { /* placeholder */ }
    composable(SEARCH_ROUTE) { /* placeholder */ }
}
