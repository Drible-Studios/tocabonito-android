package studios.drible.tocabonito.core.domain.model

data class AudioTrack(
    val index: Int,
    val name: String,
    val languageCode: String?,
) {
    val displayName: String get() = name

    companion object {
        val DISABLED = AudioTrack(index = -1, name = "Off", languageCode = null)
    }
}
