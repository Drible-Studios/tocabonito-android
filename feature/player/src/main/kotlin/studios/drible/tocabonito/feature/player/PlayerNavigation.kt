package studios.drible.tocabonito.feature.player

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val PLAYER_ROUTE = "player/{mediaId}/{streamUrl}"

fun NavGraphBuilder.playerGraph(onBack: () -> Unit) {
    composable(
        route = PLAYER_ROUTE,
        arguments = listOf(
            navArgument("mediaId") { type = NavType.StringType },
            navArgument("streamUrl") { type = NavType.StringType },
        ),
    ) {
        PlayerScreen(onBack = onBack)
    }
}

fun NavController.navigateToPlayer(mediaId: String, streamUrl: String) {
    navigate("player/$mediaId/${Uri.encode(streamUrl)}")
}
