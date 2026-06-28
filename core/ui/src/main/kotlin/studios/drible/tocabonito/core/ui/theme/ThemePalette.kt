package studios.drible.tocabonito.core.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemePalette(
    val background: Color,
    val cardBackground: Color,
    val surfaceElevated: Color,
    val accent: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val gradientBottom: Color,
    val silhouetteColor: Color,
    val isLight: Boolean,
) {
    companion object {
        val Canarinho = ThemePalette(
            background = Color(0xFFFFD100),
            cardBackground = Color(0xFF001A4D),
            surfaceElevated = Color(0xFFCCA600),
            accent = Color(0xFF009B3A),
            secondary = Color(0xFF001A4D),
            textPrimary = Color(0xFF001A4D),
            textSecondary = Color(0xFF001A4D).copy(alpha = 0.7f),
            textTertiary = Color(0xFF001A4D).copy(alpha = 0.5f),
            gradientBottom = Color(0xFFFFD100).copy(alpha = 0.85f),
            silhouetteColor = Color(0xFF001A4D),
            isLight = true,
        )

        val SelecaoAzul = ThemePalette(
            background = Color(0xFF001A4D),
            cardBackground = Color(0xFF002466),
            surfaceElevated = Color(0xFF003399),
            accent = Color(0xFFFFD100),
            secondary = Color(0xFF009C3B),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.7f),
            textTertiary = Color.White.copy(alpha = 0.5f),
            gradientBottom = Color(0xFF001A4D).copy(alpha = 0.85f),
            silhouetteColor = Color.White,
            isLight = false,
        )

        val JogaBonito = ThemePalette(
            background = Color(0xFF002E1A),
            cardBackground = Color(0xFF003D24),
            surfaceElevated = Color(0xFF004D2E),
            accent = Color(0xFFFFD100),
            secondary = Color(0xFF003399),
            textPrimary = Color(0xFFFFD100),
            textSecondary = Color.White.copy(alpha = 0.7f),
            textTertiary = Color.White.copy(alpha = 0.5f),
            gradientBottom = Color(0xFF002E1A).copy(alpha = 0.85f),
            silhouetteColor = Color(0xFFFFD100),
            isLight = false,
        )

        val DarkFlix = ThemePalette(
            background = Color(0xFF141414),
            cardBackground = Color(0xFF1F1F1F),
            surfaceElevated = Color(0xFF2E2E2E),
            accent = Color(0xFFE31E26),
            secondary = Color(0xFF4D4D4D),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.7f),
            textTertiary = Color.White.copy(alpha = 0.5f),
            gradientBottom = Color(0xFF141414).copy(alpha = 0.85f),
            silhouetteColor = Color(0xFFE31E26),
            isLight = false,
        )
    }
}

enum class AppTheme(val displayName: String, val palette: ThemePalette) {
    CANARINHO("Canarinho", ThemePalette.Canarinho),
    SELECAO_AZUL("Seleção Azul", ThemePalette.SelecaoAzul),
    JOGA_BONITO("Joga Bonito", ThemePalette.JogaBonito),
    DARKFLIX("DarkFlix", ThemePalette.DarkFlix),
}
