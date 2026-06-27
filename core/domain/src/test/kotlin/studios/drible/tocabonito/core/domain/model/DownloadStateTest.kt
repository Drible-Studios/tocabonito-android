package studios.drible.tocabonito.core.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DownloadStateTest {

    @Test
    fun `queued can transition to resolving`() {
        DownloadStateMachine.canTransition(DownloadState.QUEUED, DownloadState.RESOLVING) shouldBe true
    }

    @Test
    fun `completed cannot transition to downloading`() {
        DownloadStateMachine.canTransition(DownloadState.COMPLETED, DownloadState.DOWNLOADING) shouldBe false
    }

    @Test
    fun `downloading can transition to paused`() {
        DownloadStateMachine.canTransition(DownloadState.DOWNLOADING, DownloadState.PAUSED) shouldBe true
    }

    @Test
    fun `paused can transition to queued for resume`() {
        DownloadStateMachine.canTransition(DownloadState.PAUSED, DownloadState.QUEUED) shouldBe true
    }

    @Test
    fun `failed can transition to queued for retry`() {
        DownloadStateMachine.canTransition(DownloadState.FAILED, DownloadState.QUEUED) shouldBe true
    }
}
