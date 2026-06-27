package studios.drible.tocabonito.core.ui.theme

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ThemeProviderTest {

    private val fakeStore = FakeThemeStore()

    private fun createProvider() = ThemeProvider(fakeStore)

    @Test
    fun `defaults to canarinho when no stored value`() = runTest {
        val provider = createProvider()
        provider.selectedTheme.test {
            awaitItem() shouldBe AppTheme.CANARINHO
        }
    }

    @Test
    fun `restores stored theme on creation`() = runTest {
        fakeStore.storedValue = AppTheme.SELECAO_AZUL.name
        val provider = createProvider()
        provider.selectedTheme.test {
            awaitItem() shouldBe AppTheme.SELECAO_AZUL
        }
    }

    @Test
    fun `selectTheme persists and emits new value`() = runTest {
        val provider = createProvider()
        provider.selectedTheme.test {
            awaitItem() shouldBe AppTheme.CANARINHO
            provider.selectTheme(AppTheme.JOGA_BONITO)
            awaitItem() shouldBe AppTheme.JOGA_BONITO
        }
        fakeStore.storedValue shouldBe AppTheme.JOGA_BONITO.name
    }

    @Test
    fun `unknown stored value falls back to canarinho`() = runTest {
        fakeStore.storedValue = "campoVerde"
        val provider = createProvider()
        provider.selectedTheme.test {
            awaitItem() shouldBe AppTheme.CANARINHO
        }
    }
}

class FakeThemeStore : ThemeStore {
    var storedValue: String? = null

    override fun load(): String? = storedValue
    override fun save(value: String) { storedValue = value }
}
