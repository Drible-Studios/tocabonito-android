package studios.drible.tocabonito.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val label: String,
) {
    HOME("home", Icons.Default.Home, "Home"),
    EXPLORE("explore", Icons.Outlined.Explore, "Explore"),
    SEARCH("search", Icons.Default.Search, "Search"),
    DOWNLOADS("downloads", Icons.Default.Download, "Downloads"),
    MY_LIST("mylist", Icons.Default.FavoriteBorder, "My List"),
}
