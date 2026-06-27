package studios.drible.tocabonito.core.domain.model

data class SubtitleTrack(
    val index: Int,
    val name: String,
    val languageCode: String?,
    val codec: String? = null,
    val isExternal: Boolean = false,
    val externalUrl: String? = null,
) {
    val isBitmap: Boolean
        get() {
            val c = codec?.lowercase() ?: return false
            return c.contains("pgs") || c.contains("hdmv") || c.contains("dvdsub") || c.contains("dvb")
        }

    val displayName: String
        get() = if (isBitmap) "$name (Bitmap)" else name

    companion object {
        val DISABLED = SubtitleTrack(index = -1, name = "Off", languageCode = null)
    }
}
