package studios.drible.tocabonito.core.domain.repository

import studios.drible.tocabonito.core.domain.model.SubtitleTrack

interface SubtitleRepository {
    /**
     * Searches OpenSubtitles for the given IMDB ID, downloads the best match,
     * caches it locally, and returns a SubtitleTrack pointing to the cached file.
     * Returns null if no subtitles found or download fails.
     */
    suspend fun fetchSubtitle(imdbId: String, language: String = "pob"): SubtitleTrack?
}
