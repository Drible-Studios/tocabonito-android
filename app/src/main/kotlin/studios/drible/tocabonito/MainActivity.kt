package studios.drible.tocabonito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import studios.drible.tocabonito.core.ui.theme.ThemeProvider
import studios.drible.tocabonito.core.ui.theme.TocaBonitoTheme
import studios.drible.tocabonito.navigation.MainScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeProvider: ThemeProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val theme by themeProvider.selectedTheme.collectAsState()
            val palette = theme.palette

            LaunchedEffect(palette.isLight) {
                enableEdgeToEdge(
                    statusBarStyle = if (palette.isLight) {
                        SystemBarStyle.light(0, 0)
                    } else {
                        SystemBarStyle.dark(0)
                    },
                )
            }

            TocaBonitoTheme(palette = palette) {
                MainScreen()
            }
        }
    }
}
