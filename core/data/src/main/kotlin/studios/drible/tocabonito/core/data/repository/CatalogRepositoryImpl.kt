package studios.drible.tocabonito.core.data.repository

import studios.drible.tocabonito.core.data.api.tmdb.TMDBClient
import studios.drible.tocabonito.core.domain.model.Episode
import studios.drible.tocabonito.core.domain.model.Genre
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.YearFilter
import studios.drible.tocabonito.core.domain.repository.CatalogRepository
import javax.inject.Inject

class CatalogRepositoryImpl @Inject constructor(
    private val tmdbClient: TMDBClient,
) : CatalogRepository {

    override suspend fun trending(): List<MediaItem> =
        tmdbClient.trending()

    override suspend fun popular(type: MediaType): List<MediaItem> =
        tmdbClient.popular(type.value)

    override suspend fun topRated(type: MediaType): List<MediaItem> =
        tmdbClient.popular(type.value)

    override suspend fun search(query: String): List<MediaItem> =
        tmdbClient.search(query)

    override suspend fun details(id: String, type: MediaType): MediaItem =
        tmdbClient.details(id, type.value)

    override suspend fun seasons(seriesId: String): List<List<Episode>> =
        emptyList() // TODO: Phase 2

    override suspend fun genres(type: MediaType): List<Genre> =
        tmdbClient.genres(type.value)

    override suspend fun discover(
        genreId: Int,
        type: MediaType,
        yearFilter: YearFilter?,
        page: Int,
    ): List<MediaItem> =
        tmdbClient.discover(type.value, genreId, page)
}
