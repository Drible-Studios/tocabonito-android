package studios.drible.tocabonito.feature.detail.model

data class StreamFilters(
    val quality: String? = null,
    val source: String? = null,
    val language: String? = null,
) {
    val isActive: Boolean get() = quality != null || source != null || language != null

    companion object {
        val EMPTY = StreamFilters()
    }
}
