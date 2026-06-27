package studios.drible.tocabonito.core.data.stream

import studios.drible.tocabonito.core.domain.model.StreamOption

object StreamSorter {
    fun sort(streams: List<StreamOption>): List<StreamOption> =
        streams.sortedWith(compareBy<StreamOption> { qualityOrder(it.quality) }
            .thenByDescending { hasPtAudio(it.metadata.languages) }
            .thenBy { sourceOrder(it.metadata.source) }
            .thenBy { it.playabilityTier("mkv") }
            .thenByDescending { it.seeders })

    internal fun qualityOrder(quality: String): Int = when (quality) {
        "4K" -> 0
        "1080p" -> 1
        "720p" -> 2
        "480p" -> 3
        else -> 4
    }

    private fun hasPtAudio(languages: List<String>): Boolean =
        "PT" in languages || "DUAL" in languages || "MULTI" in languages

    private fun sourceOrder(source: String?): Int = when (source) {
        "BluRay" -> 0
        "WEB-DL" -> 1
        "WEBRip" -> 2
        "TS" -> 4
        "CAM" -> 5
        else -> 3
    }
}
