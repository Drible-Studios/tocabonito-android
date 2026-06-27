package studios.drible.tocabonito.core.testing

import studios.drible.tocabonito.core.domain.model.Episode
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamMetadata
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.domain.model.StreamQuality
import studios.drible.tocabonito.core.domain.model.WatchProgress

object TestFixtures {
    fun mediaItem(
        id: String = "tt1234567",
        title: String = "Test Movie",
        mediaType: MediaType = MediaType.MOVIE,
        releaseYear: Int = 2024,
    ) = MediaItem(
        id = id,
        title = title,
        overview = "A test movie overview",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        mediaType = mediaType,
        releaseYear = releaseYear,
        voteAverage = 7.5,
        genreIds = listOf(28, 12),
    )

    fun streamOption(
        quality: String = "1080p",
        source: String = "BluRay",
        codec: String = "H264",
        seeders: Int = 100,
    ) = StreamOption(
        title = "Movie.2024.${quality}.${source}.${codec}",
        quality = quality,
        size = "2.5 GB",
        seeders = seeders,
        metadata = StreamMetadata(
            codec = codec,
            hdr = null,
            source = source,
            languages = listOf("EN"),
            subtitles = emptyList()
        ),
        infoHash = "abc${quality}${source}",
        fileIndex = 0,
    )

    fun streamLink(
        quality: StreamQuality = StreamQuality.FULL,
    ) = StreamLink(
        id = "link1",
        fileName = "movie.mkv",
        fileSize = 2_500_000_000.toInt(),
        hlsUrl = "https://rd.example.com/hls/manifest.m3u8",
        directUrl = "https://rd.example.com/direct/movie.mkv",
        quality = quality,
    )

    fun watchProgress(
        mediaId: String = "tt1234567",
        currentTime: Double = 3600.0,
        duration: Double = 7200.0,
    ) = WatchProgress(
        id = "wp_$mediaId",
        mediaItem = mediaItem(id = mediaId),
        currentTime = currentTime,
        duration = duration,
        lastWatched = 1719500000000L,
        episodeId = null,
    )

    fun episode(
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
    ) = Episode(
        id = "ep_s${seasonNumber}e${episodeNumber}",
        name = "Episode $episodeNumber",
        overview = "Episode overview",
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        stillPath = "/still.jpg",
        airDate = "2024-01-15",
    )
}
