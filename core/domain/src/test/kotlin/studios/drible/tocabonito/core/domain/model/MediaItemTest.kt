package studios.drible.tocabonito.core.domain.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class MediaItemTest {

    @Test
    fun `posterURL constructs correct TMDB URL`() {
        val item = MediaItem(
            id = "tt1234567",
            title = "Test Movie",
            overview = "A test movie",
            posterPath = "/abc123.jpg",
            backdropPath = "/backdrop.jpg",
            mediaType = MediaType.MOVIE,
            releaseYear = 2024,
            voteAverage = 7.5,
            genreIds = listOf(28, 12),
        )
        item.posterUrl shouldContain "image.tmdb.org/t/p/w500/abc123.jpg"
    }

    @Test
    fun `posterURL is null when posterPath is null`() {
        val item = MediaItem(
            id = "tt1234567",
            title = "Test",
            overview = "",
            posterPath = null,
            backdropPath = null,
            mediaType = MediaType.MOVIE,
            releaseYear = 2024,
            voteAverage = 0.0,
            genreIds = emptyList(),
        )
        item.posterUrl shouldBe null
    }

    @Test
    fun `isMovie returns true for movie type`() {
        val item = MediaItem(
            id = "tt1", title = "", overview = "", posterPath = null,
            backdropPath = null, mediaType = MediaType.MOVIE,
            releaseYear = 2024, voteAverage = 0.0, genreIds = emptyList(),
        )
        item.isMovie shouldBe true
    }

    @Test
    fun `isMovie returns false for series type`() {
        val item = MediaItem(
            id = "tt1", title = "", overview = "", posterPath = null,
            backdropPath = null, mediaType = MediaType.SERIES,
            releaseYear = 2024, voteAverage = 0.0, genreIds = emptyList(),
        )
        item.isMovie shouldBe false
    }
}
