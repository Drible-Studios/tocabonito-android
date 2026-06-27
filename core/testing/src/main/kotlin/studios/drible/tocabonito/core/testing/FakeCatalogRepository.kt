package studios.drible.tocabonito.core.testing

import studios.drible.tocabonito.core.domain.model.Episode
import studios.drible.tocabonito.core.domain.model.Genre
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.YearFilter
import studios.drible.tocabonito.core.domain.repository.CatalogRepository

class FakeCatalogRepository : CatalogRepository {
    var trendingResult: List<MediaItem> = emptyList()
    var popularResult: List<MediaItem> = emptyList()
    var topRatedResult: List<MediaItem> = emptyList()
    var searchResult: List<MediaItem> = emptyList()
    var detailsResult: MediaItem? = null
    var seasonsResult: List<List<Episode>> = emptyList()
    var genresResult: List<Genre> = emptyList()
    var discoverResult: List<MediaItem> = emptyList()
    var shouldThrow: Exception? = null

    override suspend fun trending(): List<MediaItem> {
        shouldThrow?.let { throw it }
        return trendingResult
    }

    override suspend fun popular(type: MediaType): List<MediaItem> {
        shouldThrow?.let { throw it }
        return popularResult
    }

    override suspend fun topRated(type: MediaType): List<MediaItem> {
        shouldThrow?.let { throw it }
        return topRatedResult
    }

    override suspend fun search(query: String): List<MediaItem> {
        shouldThrow?.let { throw it }
        return searchResult
    }

    override suspend fun details(id: String, type: MediaType): MediaItem {
        shouldThrow?.let { throw it }
        return detailsResult ?: throw NoSuchElementException("No details configured")
    }

    override suspend fun seasons(seriesId: String): List<List<Episode>> {
        shouldThrow?.let { throw it }
        return seasonsResult
    }

    override suspend fun genres(type: MediaType): List<Genre> {
        shouldThrow?.let { throw it }
        return genresResult
    }

    override suspend fun discover(
        genreId: Int,
        type: MediaType,
        yearFilter: YearFilter?,
        page: Int
    ): List<MediaItem> {
        shouldThrow?.let { throw it }
        return discoverResult
    }
}
