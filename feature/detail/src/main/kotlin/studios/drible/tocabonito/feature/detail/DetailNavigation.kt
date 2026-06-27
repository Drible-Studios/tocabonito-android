package studios.drible.tocabonito.feature.detail

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val DETAIL_ROUTE = "detail/{mediaId}/{mediaType}"

fun NavGraphBuilder.detailGraph(
    onNavigateToPlayer: (mediaId: String, streamUrl: String) -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = DETAIL_ROUTE,
        arguments = listOf(
            navArgument("mediaId") { type = NavType.StringType },
            navArgument("mediaType") { type = NavType.StringType },
        ),
    ) {
        DetailScreen(
            onNavigateToPlayer = onNavigateToPlayer,
            onBack = onBack,
        )
    }
}

fun detailRoute(mediaId: String, mediaType: String): String = "detail/$mediaId/$mediaType"
