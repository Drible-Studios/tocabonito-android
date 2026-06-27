package studios.drible.tocabonito.core.ui.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ThemePaletteColorTest {

    @Nested
    inner class CanarinhoFixes {
        @Test
        fun `surfaceElevated uses dark navy not olive`() {
            ThemePalette.Canarinho.surfaceElevated shouldBe Color(0xFF002466)
        }

        @Test
        fun `textSecondary uses 80 percent opacity navy`() {
            ThemePalette.Canarinho.textSecondary shouldBe Color(0xCC001A4D)
        }

        @Test
        fun `textTertiary uses 60 percent opacity navy`() {
            ThemePalette.Canarinho.textTertiary shouldBe Color(0x99001A4D)
        }

        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.Canarinho.gradientBottom shouldBe Color(0xF2FFD100)
        }
    }

    @Nested
    inner class SelecaoAzulFixes {
        @Test
        fun `textSecondary uses 80 percent alpha not 75`() {
            ThemePalette.SelecaoAzul.textSecondary shouldBe Color.White.copy(alpha = 0.80f)
        }

        @Test
        fun `secondary is 009C3B not 009B3A`() {
            ThemePalette.SelecaoAzul.secondary shouldBe Color(0xFF009C3B)
        }

        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.SelecaoAzul.gradientBottom shouldBe Color(0xF2001A4D)
        }
    }

    @Nested
    inner class JogaBonitoFixes {
        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.JogaBonito.gradientBottom shouldBe Color(0xF2002E1A)
        }
    }

    @Nested
    inner class DarkFlixFixes {
        @Test
        fun `textSecondary uses 80 percent alpha not 75`() {
            ThemePalette.DarkFlix.textSecondary shouldBe Color.White.copy(alpha = 0.80f)
        }

        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.DarkFlix.gradientBottom shouldBe Color(0xF2141414)
        }
    }
}
