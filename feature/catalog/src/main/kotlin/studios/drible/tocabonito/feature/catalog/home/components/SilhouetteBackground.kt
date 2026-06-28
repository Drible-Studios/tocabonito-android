package studios.drible.tocabonito.feature.catalog.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

@Composable
fun SilhouetteBackground(modifier: Modifier = Modifier) {
    val color = LocalThemePalette.current.silhouetteColor
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawOval(
            color = color.copy(alpha = 0.03f),
            topLeft = Offset(-w * 0.2f, h * 0.1f),
            size = Size(w * 0.8f, h * 0.5f),
        )
        drawOval(
            color = color.copy(alpha = 0.04f),
            topLeft = Offset(w * 0.5f, h * 0.3f),
            size = Size(w * 0.7f, h * 0.4f),
        )
        drawOval(
            color = color.copy(alpha = 0.02f),
            topLeft = Offset(w * 0.1f, h * 0.6f),
            size = Size(w * 0.9f, h * 0.45f),
        )
    }
}
