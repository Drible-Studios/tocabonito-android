package studios.drible.tocabonito.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import studios.drible.tocabonito.BuildConfig
import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.api.tmdb.TMDBClient
import studios.drible.tocabonito.core.data.api.torrentio.TorrentioClient
import studios.drible.tocabonito.core.data.db.TocaBonitoDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TocaBonitoDatabase =
        Room.databaseBuilder(context, TocaBonitoDatabase::class.java, "tocabonito.db").build()

    @Provides
    fun provideFavoriteDao(db: TocaBonitoDatabase) = db.favoriteDao()

    @Provides
    fun provideWatchProgressDao(db: TocaBonitoDatabase) = db.watchProgressDao()

    @Provides
    fun provideDownloadDao(db: TocaBonitoDatabase) = db.downloadDao()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) { level = LogLevel.NONE }
    }

    @Provides
    @Singleton
    fun provideTMDBClient(httpClient: HttpClient): TMDBClient =
        TMDBClient(httpClient, BuildConfig.TMDB_API_KEY)

    @Provides
    @Singleton
    fun provideTorrentioClient(httpClient: HttpClient): TorrentioClient =
        TorrentioClient(httpClient)

    @Provides
    @Singleton
    fun provideRealDebridClient(httpClient: HttpClient): RealDebridClient =
        RealDebridClient(httpClient, BuildConfig.REAL_DEBRID_API_KEY)
}
