package studios.drible.tocabonito.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.data.download.DefaultDownloadEnqueuer
import studios.drible.tocabonito.core.data.download.HttpDownloader
import studios.drible.tocabonito.core.data.download.KtorHttpDownloader
import studios.drible.tocabonito.core.domain.repository.DownloadEnqueuer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {

    @Binds
    abstract fun bindDownloadEnqueuer(impl: DefaultDownloadEnqueuer): DownloadEnqueuer

    @Binds
    abstract fun bindHttpDownloader(impl: KtorHttpDownloader): HttpDownloader

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)
    }
}
