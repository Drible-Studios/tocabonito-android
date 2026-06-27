package studios.drible.tocabonito.core.data.stream

import studios.drible.tocabonito.core.domain.model.StreamMetadata

object StreamMetadataParser {
    fun parse(title: String): StreamMetadata = StreamMetadata(
        codec = parseCodec(title),
        hdr = parseHDR(title),
        source = parseSource(title),
        languages = parseLanguages(title),
        subtitles = parseSubtitles(title),
    )

    private fun parseCodec(text: String): String? {
        val lower = text.lowercase()
        if (lower.contains("hevc") || lower.contains("x265") || lower.contains("h265") || lower.contains("h.265")) return "H265"
        if (lower.contains("x264") || lower.contains("h264") || lower.contains("h.264")) return "H264"
        if (lower.contains("av1")) return "AV1"
        return null
    }

    private fun parseHDR(text: String): String? {
        val lower = text.lowercase()
        if (lower.contains("dolby vision") || lower.contains(" dv") || lower.contains("|dv") || lower.contains(".dv.")) return "DV"
        if (lower.contains("hdr10+")) return "HDR10+"
        if (lower.contains("hdr10")) return "HDR10"
        if (lower.contains("hdr")) return "HDR"
        return null
    }

    private fun parseSource(text: String): String? {
        val lower = text.lowercase()
        if (lower.contains("web-dl") || lower.contains("webdl")) return "WEB-DL"
        if (lower.contains("webrip")) return "WEBRip"
        if (lower.contains("bluray") || lower.contains("blu-ray") || lower.contains("remux")) return "BluRay"
        if (lower.contains("hdts") || lower.contains("telesync") || lower.contains("telecine")) return "TS"
        if (lower.contains("cam") || lower.contains("camrip")) return "CAM"
        if (lower.contains("dcprip")) return "WEBRip"
        return null
    }

    private val flagToLanguage = mapOf(
        "🇬🇧" to "EN", "🇺🇸" to "EN",
        "🇮🇹" to "IT", "🇫🇷" to "FR", "🇩🇪" to "DE", "🇪🇸" to "ES",
        "🇧🇷" to "PT", "🇵🇹" to "PT",
        "🇷🇺" to "RU", "🇯🇵" to "JA", "🇰🇷" to "KO", "🇨🇳" to "ZH",
        "🇮🇳" to "HI", "🇳🇱" to "NL", "🇵🇱" to "PL", "🇹🇷" to "TR",
        "🇸🇪" to "SV", "🇳🇴" to "NO", "🇩🇰" to "DA", "🇫🇮" to "FI",
        "🇬🇷" to "EL", "🇷🇴" to "RO", "🇭🇺" to "HU", "🇨🇿" to "CS",
    )

    private val textToLanguage = listOf(
        "eng" to "EN", "ita" to "IT", "fre" to "FR", "fra" to "FR",
        "ger" to "DE", "deu" to "DE", "spa" to "ES", "por" to "PT",
        "rus" to "RU", "jpn" to "JA", "kor" to "KO", "chi" to "ZH",
        "dut" to "NL", "pol" to "PL", "tur" to "TR", "hin" to "HI",
    )

    private fun parseLanguages(text: String): List<String> {
        val langs = mutableListOf<String>()
        for ((flag, code) in flagToLanguage) {
            if (text.contains(flag) && code !in langs) langs.add(code)
        }
        val lower = text.lowercase()
        if (lower.contains("dual") && "DUAL" !in langs) langs.add("DUAL")
        if (lower.contains("multi") && "MULTI" !in langs) langs.add("MULTI")

        val subIndex = lower.indexOf("sub").takeIf { it >= 0 } ?: lower.length
        val audioSection = lower.substring(0, subIndex)
        for ((pattern, code) in textToLanguage) {
            if (audioSection.contains(pattern) && code !in langs) langs.add(code)
        }
        if ("PT" !in langs) {
            if (audioSection.contains("dublado") || audioSection.contains("nacional") ||
                audioSection.contains("pt-br") || audioSection.contains("pt_br") ||
                audioSection.contains("ptbr")
            ) langs.add("PT")
        }
        return langs
    }

    private fun parseSubtitles(text: String): List<String> {
        val lower = text.lowercase()
        val subIndex = lower.indexOf("sub")
        if (subIndex < 0) return emptyList()
        val subSection = lower.substring(subIndex)
        val subs = mutableListOf<String>()
        for ((pattern, code) in textToLanguage) {
            if (subSection.contains(pattern) && code !in subs) subs.add(code)
        }
        return subs
    }
}
