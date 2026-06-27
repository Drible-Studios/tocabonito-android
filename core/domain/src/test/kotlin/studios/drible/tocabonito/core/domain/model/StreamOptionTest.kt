package studios.drible.tocabonito.core.domain.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class StreamOptionTest {

    @Test
    fun `magnetLink builds correct magnet URI`() {
        val option = StreamOption(
            title = "Movie.2024.1080p.BluRay",
            quality = "1080p",
            size = "2.5 GB",
            seeders = 50,
            metadata = StreamMetadata(codec = "H264", hdr = null, source = "BluRay", languages = listOf("EN"), subtitles = emptyList()),
            infoHash = "abc123def456",
            fileIndex = 0,
            resolverUrl = null,
        )
        option.magnetLink shouldContain "xt=urn:btih:abc123def456"
    }

    @Test
    fun `id is deterministic from hash and index`() {
        val a = StreamOption("T", "1080p", "1GB", 0, StreamMetadata(null, null, null, emptyList(), emptyList()), "hash1", 0, null)
        val b = StreamOption("T", "1080p", "1GB", 0, StreamMetadata(null, null, null, emptyList(), emptyList()), "hash1", 0, null)
        a.id shouldBe b.id
    }

    @Test
    fun `playabilityTier prefers H264`() {
        val h264 = StreamOption("T", "1080p", "1GB", 0, StreamMetadata("H264", null, null, emptyList(), emptyList()), "h", 0, null)
        val hevc = StreamOption("T", "1080p", "1GB", 0, StreamMetadata("H265", null, null, emptyList(), emptyList()), "h", 0, null)
        h264.playabilityTier("mkv") shouldBe 1
        hevc.playabilityTier("mkv") shouldBe 3
    }

    @Test
    fun `playabilityTier tier 2 for matching extension`() {
        val mkv = StreamOption("Movie.mkv", "1080p", "1GB", 0, StreamMetadata(null, null, null, emptyList(), emptyList()), "h", 0, null)
        mkv.playabilityTier("mkv") shouldBe 2
    }
}
