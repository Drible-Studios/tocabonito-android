package studios.drible.tocabonito.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.ui.theme.ThemeStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    @Singleton
    fun provideThemeStore(@ApplicationContext context: Context): ThemeStore {
        val prefs = context.getSharedPreferences("tocabonito_prefs", Context.MODE_PRIVATE)
        return SharedPrefsThemeStore(prefs)
    }
}

class SharedPrefsThemeStore(private val prefs: SharedPreferences) : ThemeStore {
    override fun load(): String? = prefs.getString("selected_theme", null)
    override fun save(value: String) { prefs.edit().putString("selected_theme", value).apply() }
}
