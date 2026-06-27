package studios.drible.tocabonito.core.data.stream

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StreamMetadataParserTest {

    @Test
    fun `parses H265 codec`() {
        val result = StreamMetadataParser.parse("Movie.2024.1080p.BluRay.x265.DTS")
        result.codec shouldBe "H265"
    }

    @Test
    fun `parses H264 codec`() {
        val result = StreamMetadataParser.parse("Movie.2024.720p.WEB-DL.x264")
        result.codec shouldBe "H264"
    }

    @Test
    fun `parses BluRay source`() {
        val result = StreamMetadataParser.parse("Movie.2024.1080p.BluRay.REMUX")
        result.source shouldBe "BluRay"
    }

    @Test
    fun `parses WEB-DL source`() {
        val result = StreamMetadataParser.parse("Movie.2024.1080p.WEB-DL.DD5.1")
        result.source shouldBe "WEB-DL"
    }

    @Test
    fun `parses HDR10+ HDR type`() {
        val result = StreamMetadataParser.parse("Movie.2024.2160p.UHD.BluRay.HDR10+")
        result.hdr shouldBe "HDR10+"
    }

    @Test
    fun `parses Dolby Vision`() {
        val result = StreamMetadataParser.parse("Movie.2024.2160p.Dolby Vision")
        result.hdr shouldBe "DV"
    }

    @Test
    fun `parses PT language from Brazilian flag`() {
        val result = StreamMetadataParser.parse("Movie.2024.1080p 🇧🇷 Audio")
        result.languages shouldContain "PT"
    }

    @Test
    fun `parses DUAL language`() {
        val result = StreamMetadataParser.parse("Movie.2024.1080p.DUAL.BluRay")
        result.languages shouldContain "DUAL"
    }

    @Test
    fun `parses subtitles after sub marker`() {
        val result = StreamMetadataParser.parse("Movie.2024.1080p Sub: eng por")
        result.subtitles shouldContain "EN"
        result.subtitles shouldContain "PT"
    }
}
