package studios.drible.tocabonito.core.ui.theme

import org.junit.jupiter.api.Test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ThemePaletteTest {

    @Test
    fun `all four palettes are distinct`() {
        val palettes = listOf(
            ThemePalette.Canarinho,
            ThemePalette.SelecaoAzul,
            ThemePalette.JogaBonito,
            ThemePalette.DarkFlix,
        )
        palettes.distinct().size shouldBe 4
    }

    @Test
    fun `canarinho is light scheme`() {
        ThemePalette.Canarinho.isLight shouldBe true
    }

    @Test
    fun `selecao azul is dark scheme`() {
        ThemePalette.SelecaoAzul.isLight shouldBe false
    }

    @Test
    fun `joga bonito is dark scheme`() {
        ThemePalette.JogaBonito.isLight shouldBe false
    }

    @Test
    fun `darkflix is dark scheme`() {
        ThemePalette.DarkFlix.isLight shouldBe false
    }

    @Test
    fun `all palettes have non-null silhouette color`() {
        AppTheme.entries.forEach { theme ->
            theme.palette.silhouetteColor shouldNotBe null
        }
    }

    @Test
    fun `AppTheme entries match expected count`() {
        AppTheme.entries.size shouldBe 4
    }
}
