package studios.drible.tocabonito.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.data.repository.CatalogRepositoryImpl
import studios.drible.tocabonito.core.data.repository.FavoritesRepositoryImpl
import studios.drible.tocabonito.core.data.repository.ProgressRepositoryImpl
import studios.drible.tocabonito.core.data.repository.StreamRepositoryImpl
import studios.drible.tocabonito.core.domain.repository.CatalogRepository
import studios.drible.tocabonito.core.domain.repository.FavoritesRepository
import studios.drible.tocabonito.core.domain.repository.ProgressRepository
import studios.drible.tocabonito.core.domain.repository.StreamRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    abstract fun bindStreamRepository(impl: StreamRepositoryImpl): StreamRepository

    @Binds
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository
}
