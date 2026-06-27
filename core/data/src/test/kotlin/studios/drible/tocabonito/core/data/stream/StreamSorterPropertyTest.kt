package studios.drible.tocabonito.core.data.stream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import studios.drible.tocabonito.core.domain.model.StreamMetadata
import studios.drible.tocabonito.core.domain.model.StreamOption

class StreamSorterPropertyTest : FunSpec({

    val qualities = listOf("4K", "1080p", "720p", "480p")
    val sources = listOf("BluRay", "WEB-DL", "WEBRip", null)

    val arbStreamOption = Arb.int(0..100).map { seeders ->
        val quality = qualities.random()
        val source = sources.random()
        StreamOption(
            title = "Movie.$quality.${source ?: "Unknown"}",
            quality = quality,
            size = "1 GB",
            seeders = seeders,
            metadata = StreamMetadata(null, null, source, listOf("EN"), emptyList()),
            infoHash = "hash_${System.nanoTime()}",
            fileIndex = 0,
        )
    }

    test("sorted output has same elements as input") {
        checkAll(Arb.list(arbStreamOption, 1..20)) { streams ->
            val sorted = StreamSorter.sort(streams)
            sorted.size shouldBe streams.size
            sorted.toSet() shouldBe streams.toSet()
        }
    }

    test("higher quality always sorts before lower quality") {
        checkAll(Arb.list(arbStreamOption, 2..10)) { streams ->
            val sorted = StreamSorter.sort(streams)
            for (i in 0 until sorted.size - 1) {
                val aQ = StreamSorter.qualityOrder(sorted[i].quality)
                val bQ = StreamSorter.qualityOrder(sorted[i + 1].quality)
                if (aQ != bQ) {
                    assert(aQ <= bQ) { "Quality order violated: ${sorted[i].quality} vs ${sorted[i + 1].quality}" }
                }
            }
        }
    }
})
