package studios.drible.tocabonito.core.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WatchProgressTest {

    private fun progress(currentTime: Double, duration: Double) = WatchProgress(
        id = "wp1",
        mediaItem = MediaItem("tt1", "T", "", null, null, MediaType.MOVIE, 2024, 0.0, emptyList()),
        currentTime = currentTime,
        duration = duration,
        lastWatched = 1719500000000L,
        episodeId = null,
    )

    @Test
    fun `percentComplete calculates correctly`() {
        progress(60.0, 120.0).percentComplete shouldBe 0.5
    }

    @Test
    fun `percentComplete is zero when duration is zero`() {
        progress(60.0, 0.0).percentComplete shouldBe 0.0
    }

    @Test
    fun `shouldShowInContinueWatching true when past 2min and under 90 percent`() {
        progress(130.0, 7200.0).shouldShowInContinueWatching shouldBe true
    }

    @Test
    fun `shouldShowInContinueWatching false when under 2min`() {
        progress(60.0, 7200.0).shouldShowInContinueWatching shouldBe false
    }

    @Test
    fun `isFinished true at 90 percent`() {
        progress(108.0, 120.0).isFinished shouldBe true
    }
}
