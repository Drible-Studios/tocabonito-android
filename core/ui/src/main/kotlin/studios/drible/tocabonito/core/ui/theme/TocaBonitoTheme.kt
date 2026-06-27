package studios.drible.tocabonito.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalThemePalette = staticCompositionLocalOf { ThemePalette.Canarinho }

@Composable
fun TocaBonitoTheme(
    palette: ThemePalette = ThemePalette.Canarinho,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.secondary,
            background = palette.background,
            surface = palette.cardBackground,
            onPrimary = palette.textPrimary,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
        )
    } else {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.secondary,
            background = palette.background,
            surface = palette.cardBackground,
            onPrimary = palette.background,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
        )
    }

    CompositionLocalProvider(LocalThemePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TocaBonitoTypography,
            content = content,
        )
    }
}
