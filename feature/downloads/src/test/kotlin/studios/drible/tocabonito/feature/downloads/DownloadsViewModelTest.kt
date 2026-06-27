package studios.drible.tocabonito.feature.downloads

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadPriority
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.repository.DownloadRepository

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeEach { Dispatchers.setMain(testDispatcher) }
    afterEach { Dispatchers.resetMain() }

    fun fakeItem(id: String, state: DownloadState) = DownloadItem(
        id = id,
        mediaId = "media-$id",
        episodeId = null,
        seasonNumber = null,
        episodeNumber = null,
        title = "Title $id",
        posterPath = null,
        backdropPath = null,
        mediaType = MediaType.MOVIE,
        quality = "1080p",
        source = "realdebrid",
        codec = "H264",
        state = state,
        progress = 0.5,
        bytesWritten = 500L,
        totalBytes = 1000L,
        estimatedBytes = 1000L,
        localFilePath = null,
        fileExtension = "mp4",
        dateQueued = System.currentTimeMillis(),
        dateCompleted = null,
        failureCount = 0,
        lastError = null,
        priority = DownloadPriority.USER_INITIATED,
        allowedOnCellular = false,
        speedBytesPerSecond = null,
    )

    test("uiState is Empty when repository emits empty list") {
        val flow = MutableStateFlow<List<DownloadItem>>(emptyList())
        val repo = object : DownloadRepository {
            override fun observeAll() = flow
            override fun observeActive() = flow
            override fun observeCompleted() = flow
            override suspend fun get(downloadId: String) = null
            override suspend fun getByMedia(mediaId: String, episodeId: String?) = null
            override suspend fun save(item: DownloadItem) {}
            override suspend fun delete(downloadId: String) {}
            override suspend fun updateState(downloadId: String, state: DownloadState) {}
            override suspend fun updateProgress(downloadId: String, progress: Double, bytesWritten: Long, speed: Double) {}
            override suspend fun updateError(downloadId: String, failureCount: Int, lastError: String?) {}
            override suspend fun updateLocalPath(downloadId: String, localFilePath: String) {}
            override suspend fun totalStorageUsed() = 0L
        }
        val viewModel = DownloadsViewModel(repo)
        viewModel.uiState.test {
            val state = awaitItem()
            state shouldBe DownloadsUiState.Empty
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("uiState splits items into active, completed, failed") {
        val items = listOf(
            fakeItem("1", DownloadState.DOWNLOADING),
            fakeItem("2", DownloadState.COMPLETED),
            fakeItem("3", DownloadState.FAILED),
        )
        val flow = MutableStateFlow(items)
        val repo = object : DownloadRepository {
            override fun observeAll() = flow
            override fun observeActive() = flow
            override fun observeCompleted() = flow
            override suspend fun get(downloadId: String) = null
            override suspend fun getByMedia(mediaId: String, episodeId: String?) = null
            override suspend fun save(item: DownloadItem) {}
            override suspend fun delete(downloadId: String) {}
            override suspend fun updateState(downloadId: String, state: DownloadState) {}
            override suspend fun updateProgress(downloadId: String, progress: Double, bytesWritten: Long, speed: Double) {}
            override suspend fun updateError(downloadId: String, failureCount: Int, lastError: String?) {}
            override suspend fun updateLocalPath(downloadId: String, localFilePath: String) {}
            override suspend fun totalStorageUsed() = 0L
        }
        val viewModel = DownloadsViewModel(repo)
        viewModel.uiState.test {
            awaitItem() // skip Empty initial
            testDispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            state.shouldBeInstanceOf<DownloadsUiState.Content>()
            (state as DownloadsUiState.Content).active.size shouldBe 1
            state.completed.size shouldBe 1
            state.failed.size shouldBe 1
            cancelAndIgnoreRemainingEvents()
        }
    }
})
