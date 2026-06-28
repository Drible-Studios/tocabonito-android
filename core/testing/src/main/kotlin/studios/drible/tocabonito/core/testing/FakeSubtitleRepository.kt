package studios.drible.tocabonito.core.testing

import studios.drible.tocabonito.core.domain.model.SubtitleTrack
import studios.drible.tocabonito.core.domain.repository.SubtitleRepository

class FakeSubtitleRepository : SubtitleRepository {
    var subtitleToReturn: SubtitleTrack? = null
    var fetchCallCount = 0
    var lastImdbId: String? = null

    override suspend fun fetchSubtitle(imdbId: String, language: String): SubtitleTrack? {
        fetchCallCount++
        lastImdbId = imdbId
        return subtitleToReturn
    }
}
