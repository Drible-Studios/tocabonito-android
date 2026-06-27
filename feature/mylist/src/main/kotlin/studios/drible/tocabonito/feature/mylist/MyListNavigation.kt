package studios.drible.tocabonito.feature.mylist

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import studios.drible.tocabonito.core.domain.model.MediaItem

const val MY_LIST_ROUTE = "mylist"

fun NavGraphBuilder.myListScreen(onItemClick: (MediaItem) -> Unit) {
    composable(MY_LIST_ROUTE) { MyListScreen(onItemClick = onItemClick) }
}
