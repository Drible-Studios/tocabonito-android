# Plan 03 — Download Pipeline (Issue #10)

**Scope:** Wire `DetailIntent.DownloadStream` through ViewModel logic to DownloadRepository + WorkManager enqueue.

---

## Files

| Action | Path | Purpose |
|--------|------|---------|
| Modify | `feature/detail/src/main/kotlin/.../DetailIntent.kt` | Add `DownloadStream` intent |
| Modify | `feature/detail/src/main/kotlin/.../DetailUiState.kt` | Add `downloadStates`, `pendingLargeDownload` |
| Modify | `feature/detail/src/main/kotlin/.../DetailViewModel.kt` | Implement `downloadStream()` |
| Create | `core/downloads/src/main/kotlin/.../DownloadWorker.kt` | WorkManager CoroutineWorker |
| Create | `core/downloads/src/main/kotlin/.../DownloadNotificationHelper.kt` | Notification channel + builder |
| Create | `core/downloads/src/main/kotlin/.../DownloadEnqueuer.kt` | Interface to enqueue work |
| Create | `core/downloads/src/main/kotlin/.../DefaultDownloadEnqueuer.kt` | WorkManager impl |
| Test | `feature/detail/src/test/kotlin/.../DetailViewModelDownloadTest.kt` | ViewModel unit tests |
| Test | `core/downloads/src/test/kotlin/.../DownloadWorkerTest.kt` | Worker unit tests |

---

## Interfaces

### Consumes

- `StreamRepository.resolveStream(option: StreamOption): StreamLink`
- `DownloadRepository.save(item: DownloadItem)`
- `DownloadRepository.updateState(id, state)`
- `DownloadRepository.updateProgress(id, downloadedBytes)`
- `DownloadRepository.updateLocalPath(id, path)`

### Produces

- `DownloadEnqueuer` interface (new)
- `DetailIntent.DownloadStream`
- `DetailIntent.ConfirmLargeDownload`
- `DetailIntent.DismissLargeDownload`

---

## TDD Steps

### Step 1 — ViewModel: small file triggers resolve + enqueue

- [ ] **RED:** Write `DetailViewModelDownloadTest`

```kotlin
// feature/detail/src/test/kotlin/studios/drible/tocabonito/feature/detail/DetailViewModelDownloadTest.kt
package studios.drible.tocabonito.feature.detail

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.testing.MainDispatcherRule

class DetailViewModelDownloadTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeStreamRepository: FakeStreamRepository
    private lateinit var fakeDownloadRepository: FakeDownloadRepository
    private lateinit var fakeDownloadEnqueuer: FakeDownloadEnqueuer
    private lateinit var viewModel: DetailViewModel

    @Before
    fun setup() {
        fakeStreamRepository = FakeStreamRepository()
        fakeDownloadRepository = FakeDownloadRepository()
        fakeDownloadEnqueuer = FakeDownloadEnqueuer()
        viewModel = DetailViewModel(
            streamRepository = fakeStreamRepository,
            downloadRepository = fakeDownloadRepository,
            downloadEnqueuer = fakeDownloadEnqueuer,
            savedStateHandle = savedStateHandleOf("mediaId" to "tt1234"),
        )
    }

    @Test
    fun `downloadStream with small file resolves and enqueues`() = runTest {
        val option = StreamOption(id = "opt1", quality = "1080p", sizeBytes = 500_000_000L)
        val streamLink = StreamLink(
            directUrl = "https://cdn.example.com/file.mp4",
            fileName = "movie.mp4",
            fileSize = 500_000_000L,
        )
        fakeStreamRepository.streamLinkToReturn = streamLink

        viewModel.uiState.test {
            // skip initial states until Success
            val initial = awaitSuccessState()

            viewModel.onIntent(DetailIntent.DownloadStream(option))

            // Should transition to RESOLVING
            val resolving = awaitSuccessState()
            assertEquals(DownloadState.RESOLVING, resolving.downloadStates["opt1"])

            // Should transition to QUEUED after resolve completes
            val queued = awaitSuccessState()
            assertEquals(DownloadState.QUEUED, queued.downloadStates["opt1"])

            // Verify repository got the item
            val saved = fakeDownloadRepository.savedItems.first()
            assertEquals("movie.mp4", saved.fileName)
            assertEquals(500_000_000L, saved.fileSize)
            assertEquals(DownloadState.QUEUED, saved.state)

            // Verify enqueuer was called
            assertEquals(1, fakeDownloadEnqueuer.enqueuedIds.size)
            assertEquals(saved.id, fakeDownloadEnqueuer.enqueuedIds.first())
        }
    }
}
```

- [ ] **GREEN:** Add `DownloadStream` intent, `downloadStates` to state, implement `downloadStream()`:

```kotlin
// In DetailIntent.kt — add:
data class DownloadStream(val option: StreamOption) : DetailIntent

// In DetailUiState.kt — add to Success:
val downloadStates: Map<String, DownloadState> = emptyMap(),
val pendingLargeDownload: StreamOption? = null,

// In DetailViewModel.kt — add:
private fun downloadStream(option: StreamOption) {
    viewModelScope.launch {
        updateDownloadState(option.id, DownloadState.RESOLVING)
        try {
            val link = streamRepository.resolveStream(option)
            val item = DownloadItem(
                id = UUID.randomUUID().toString(),
                mediaId = currentMediaId,
                title = currentTitle,
                posterPath = currentPosterPath,
                fileName = link.fileName,
                fileSize = link.fileSize,
                state = DownloadState.QUEUED,
            )
            downloadRepository.save(item)
            downloadEnqueuer.enqueue(item.id)
            updateDownloadState(option.id, DownloadState.QUEUED)
        } catch (e: Exception) {
            updateDownloadState(option.id, DownloadState.FAILED)
        }
    }
}

private fun updateDownloadState(optionId: String, state: DownloadState) {
    _uiState.update { current ->
        if (current is DetailUiState.Success) {
            current.copy(downloadStates = current.downloadStates + (optionId to state))
        } else current
    }
}
```

- [ ] **REFACTOR:** Extract UUID generation to injectable `IdGenerator` interface for testability.

**Commit:** `feat(detail): add DownloadStream intent with resolve-and-enqueue flow`

---

### Step 2 — ViewModel: large file (>2GB) shows confirmation

- [ ] **RED:**

```kotlin
@Test
fun `downloadStream with large file sets pendingLargeDownload`() = runTest {
    val option = StreamOption(id = "opt2", quality = "4K", sizeBytes = 3_000_000_000L)

    viewModel.uiState.test {
        awaitSuccessState()

        viewModel.onIntent(DetailIntent.DownloadStream(option))

        val state = awaitSuccessState()
        assertEquals(option, state.pendingLargeDownload)
        // Should NOT have resolved or enqueued
        assertNull(state.downloadStates["opt2"])
        assertTrue(fakeDownloadRepository.savedItems.isEmpty())
    }
}

@Test
fun `confirmLargeDownload proceeds with download`() = runTest {
    val option = StreamOption(id = "opt2", quality = "4K", sizeBytes = 3_000_000_000L)
    val streamLink = StreamLink(
        directUrl = "https://cdn.example.com/4k.mp4",
        fileName = "movie_4k.mp4",
        fileSize = 3_000_000_000L,
    )
    fakeStreamRepository.streamLinkToReturn = streamLink

    viewModel.uiState.test {
        awaitSuccessState()
        viewModel.onIntent(DetailIntent.DownloadStream(option))
        awaitSuccessState() // pendingLargeDownload set

        viewModel.onIntent(DetailIntent.ConfirmLargeDownload)

        val resolving = awaitSuccessState()
        assertNull(resolving.pendingLargeDownload)
        assertEquals(DownloadState.RESOLVING, resolving.downloadStates["opt2"])

        val queued = awaitSuccessState()
        assertEquals(DownloadState.QUEUED, queued.downloadStates["opt2"])
    }
}

@Test
fun `dismissLargeDownload clears pending`() = runTest {
    val option = StreamOption(id = "opt2", quality = "4K", sizeBytes = 3_000_000_000L)

    viewModel.uiState.test {
        awaitSuccessState()
        viewModel.onIntent(DetailIntent.DownloadStream(option))
        awaitSuccessState()

        viewModel.onIntent(DetailIntent.DismissLargeDownload)

        val state = awaitSuccessState()
        assertNull(state.pendingLargeDownload)
    }
}
```

- [ ] **GREEN:**

```kotlin
// In DetailIntent.kt — add:
data object ConfirmLargeDownload : DetailIntent
data object DismissLargeDownload : DetailIntent

// In DetailViewModel.kt — modify downloadStream():
private fun downloadStream(option: StreamOption) {
    val twoGb = 2L * 1024 * 1024 * 1024
    if (option.sizeBytes > twoGb) {
        _uiState.update { current ->
            if (current is DetailUiState.Success) {
                current.copy(pendingLargeDownload = option)
            } else current
        }
        return
    }
    executeDownload(option)
}

private fun confirmLargeDownload() {
    val pending = (_uiState.value as? DetailUiState.Success)?.pendingLargeDownload ?: return
    _uiState.update { current ->
        if (current is DetailUiState.Success) current.copy(pendingLargeDownload = null)
        else current
    }
    executeDownload(pending)
}

private fun dismissLargeDownload() {
    _uiState.update { current ->
        if (current is DetailUiState.Success) current.copy(pendingLargeDownload = null)
        else current
    }
}

private fun executeDownload(option: StreamOption) {
    viewModelScope.launch {
        updateDownloadState(option.id, DownloadState.RESOLVING)
        try {
            val link = streamRepository.resolveStream(option)
            val item = DownloadItem(
                id = idGenerator.generate(),
                mediaId = currentMediaId,
                title = currentTitle,
                posterPath = currentPosterPath,
                fileName = link.fileName,
                fileSize = link.fileSize,
                state = DownloadState.QUEUED,
            )
            downloadRepository.save(item)
            downloadEnqueuer.enqueue(item.id)
            updateDownloadState(option.id, DownloadState.QUEUED)
        } catch (e: Exception) {
            updateDownloadState(option.id, DownloadState.FAILED)
        }
    }
}
```

- [ ] **REFACTOR:** None needed.

**Commit:** `feat(detail): gate downloads >2GB behind confirmation dialog`

---

### Step 3 — ViewModel: resolve failure sets FAILED state

- [ ] **RED:**

```kotlin
@Test
fun `downloadStream sets FAILED when resolve throws`() = runTest {
    val option = StreamOption(id = "opt3", quality = "720p", sizeBytes = 100_000_000L)
    fakeStreamRepository.shouldThrow = IOException("Network error")

    viewModel.uiState.test {
        awaitSuccessState()

        viewModel.onIntent(DetailIntent.DownloadStream(option))

        val resolving = awaitSuccessState()
        assertEquals(DownloadState.RESOLVING, resolving.downloadStates["opt3"])

        val failed = awaitSuccessState()
        assertEquals(DownloadState.FAILED, failed.downloadStates["opt3"])
        assertTrue(fakeDownloadRepository.savedItems.isEmpty())
    }
}
```

- [ ] **GREEN:** Already handled by the `catch` block in `executeDownload()`.

- [ ] **REFACTOR:** None needed.

**Commit:** `test(detail): verify FAILED state on stream resolve failure`

---

### Step 4 — DownloadEnqueuer interface + WorkManager impl

- [ ] **RED:**

```kotlin
// core/downloads/src/test/kotlin/.../DefaultDownloadEnqueuerTest.kt
package studios.drible.tocabonito.core.downloads

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DefaultDownloadEnqueuerTest {

    private val workManager: WorkManager = mockk(relaxed = true)
    private val enqueuer = DefaultDownloadEnqueuer(workManager)

    @Test
    fun `enqueue creates unique work with downloadId input`() = runTest {
        enqueuer.enqueue("dl-123")

        val requestSlot = slot<OneTimeWorkRequest>()
        verify {
            workManager.enqueueUniqueWork(
                eq("download_dl-123"),
                eq(ExistingWorkPolicy.KEEP),
                capture(requestSlot),
            )
        }
        val inputData = requestSlot.captured.workSpec.input
        assertEquals("dl-123", inputData.getString("download_id"))
    }
}
```

- [ ] **GREEN:**

```kotlin
// core/downloads/src/main/kotlin/.../DownloadEnqueuer.kt
package studios.drible.tocabonito.core.downloads

interface DownloadEnqueuer {
    suspend fun enqueue(downloadId: String)
}

// core/downloads/src/main/kotlin/.../DefaultDownloadEnqueuer.kt
package studios.drible.tocabonito.core.downloads

import androidx.work.*
import javax.inject.Inject

class DefaultDownloadEnqueuer @Inject constructor(
    private val workManager: WorkManager,
) : DownloadEnqueuer {

    override suspend fun enqueue(downloadId: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("download_id" to downloadId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_$downloadId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
```

- [ ] **REFACTOR:** None needed.

**Commit:** `feat(downloads): add DownloadEnqueuer interface and WorkManager implementation`

---

### Step 5 — DownloadWorker: success path

- [ ] **RED:**

```kotlin
// core/downloads/src/test/kotlin/.../DownloadWorkerTest.kt
package studios.drible.tocabonito.core.downloads

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadState

@RunWith(RobolectricTestRunner::class)
class DownloadWorkerTest {

    private lateinit var context: Context
    private lateinit var fakeDownloadRepository: FakeDownloadRepository
    private lateinit var fakeHttpDownloader: FakeHttpDownloader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeDownloadRepository = FakeDownloadRepository()
        fakeHttpDownloader = FakeHttpDownloader()
    }

    @Test
    fun `worker downloads file and updates state to COMPLETED`() = runTest {
        val item = DownloadItem(
            id = "dl-1",
            mediaId = "tt1234",
            title = "Test Movie",
            posterPath = null,
            fileName = "movie.mp4",
            fileSize = 5_000_000L,
            state = DownloadState.QUEUED,
        )
        fakeDownloadRepository.savedItems.add(item)
        fakeHttpDownloader.bytesToServe = 5_000_000L

        val worker = TestListenableWorkerBuilder<DownloadWorker>(context)
            .setInputData(workDataOf("download_id" to "dl-1"))
            .build()
            .also {
                it.downloadRepository = fakeDownloadRepository
                it.httpDownloader = fakeHttpDownloader
            }

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertEquals(DownloadState.COMPLETED, fakeDownloadRepository.stateUpdates["dl-1"])
        assertNotNull(fakeDownloadRepository.pathUpdates["dl-1"])
    }

    @Test
    fun `worker updates progress every 1MB`() = runTest {
        val item = DownloadItem(
            id = "dl-2",
            mediaId = "tt1234",
            title = "Test Movie",
            posterPath = null,
            fileName = "movie.mp4",
            fileSize = 3_000_000L,
            state = DownloadState.QUEUED,
        )
        fakeDownloadRepository.savedItems.add(item)
        fakeHttpDownloader.bytesToServe = 3_000_000L

        val worker = TestListenableWorkerBuilder<DownloadWorker>(context)
            .setInputData(workDataOf("download_id" to "dl-2"))
            .build()
            .also {
                it.downloadRepository = fakeDownloadRepository
                it.httpDownloader = fakeHttpDownloader
            }

        worker.doWork()

        // 3MB file -> progress updates at 1MB and 2MB (2 updates)
        assertEquals(2, fakeDownloadRepository.progressUpdates["dl-2"]?.size ?: 0)
    }

    @Test
    fun `worker returns retry on failure with attempts remaining`() = runTest {
        val item = DownloadItem(
            id = "dl-3",
            mediaId = "tt1234",
            title = "Test Movie",
            posterPath = null,
            fileName = "movie.mp4",
            fileSize = 1_000_000L,
            state = DownloadState.QUEUED,
        )
        fakeDownloadRepository.savedItems.add(item)
        fakeHttpDownloader.shouldThrow = true

        val worker = TestListenableWorkerBuilder<DownloadWorker>(context)
            .setInputData(workDataOf("download_id" to "dl-3"))
            .setRunAttemptCount(0)
            .build()
            .also {
                it.downloadRepository = fakeDownloadRepository
                it.httpDownloader = fakeHttpDownloader
            }

        val result = worker.doWork()

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `worker returns failure after 3 attempts`() = runTest {
        val item = DownloadItem(
            id = "dl-4",
            mediaId = "tt1234",
            title = "Test Movie",
            posterPath = null,
            fileName = "movie.mp4",
            fileSize = 1_000_000L,
            state = DownloadState.QUEUED,
        )
        fakeDownloadRepository.savedItems.add(item)
        fakeHttpDownloader.shouldThrow = true

        val worker = TestListenableWorkerBuilder<DownloadWorker>(context)
            .setInputData(workDataOf("download_id" to "dl-4"))
            .setRunAttemptCount(3)
            .build()
            .also {
                it.downloadRepository = fakeDownloadRepository
                it.httpDownloader = fakeHttpDownloader
            }

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        assertEquals(DownloadState.FAILED, fakeDownloadRepository.stateUpdates["dl-4"])
    }
}
```

- [ ] **GREEN:**

```kotlin
// core/downloads/src/main/kotlin/.../HttpDownloader.kt
package studios.drible.tocabonito.core.downloads

import java.io.File

interface HttpDownloader {
    suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (bytesDownloaded: Long) -> Unit,
    )
}

// core/downloads/src/main/kotlin/.../DownloadWorker.kt
package studios.drible.tocabonito.core.downloads

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.repository.DownloadRepository
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    var downloadRepository: DownloadRepository,
    var httpDownloader: HttpDownloader,
    private val notificationHelper: DownloadNotificationHelper,
) : CoroutineWorker(context, params) {

    companion object {
        private const val MAX_RETRIES = 3
        private const val PROGRESS_INTERVAL_BYTES = 1_048_576L // 1MB
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        val item = downloadRepository.getById(downloadId) ?: return Result.failure()

        setForeground(createForegroundInfo(item.title))
        downloadRepository.updateState(downloadId, DownloadState.DOWNLOADING)

        return try {
            val destDir = File(applicationContext.filesDir, "downloads")
            destDir.mkdirs()
            val destFile = File(destDir, item.fileName)

            var lastReportedMb = 0L
            httpDownloader.download(item.fileName, destFile) { bytesDownloaded ->
                val currentMb = bytesDownloaded / PROGRESS_INTERVAL_BYTES
                if (currentMb > lastReportedMb) {
                    lastReportedMb = currentMb
                    downloadRepository.updateProgress(downloadId, bytesDownloaded)
                }
            }

            downloadRepository.updateState(downloadId, DownloadState.COMPLETED)
            downloadRepository.updateLocalPath(downloadId, destFile.absolutePath)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_RETRIES) {
                downloadRepository.updateState(downloadId, DownloadState.FAILED)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        return notificationHelper.createForegroundInfo(id, title)
    }
}
```

- [ ] **REFACTOR:** Extract download directory path to a config/provider.

**Commit:** `feat(downloads): implement DownloadWorker with progress tracking and retry`

---

### Step 6 — DownloadNotificationHelper

- [ ] **RED:**

```kotlin
// core/downloads/src/test/kotlin/.../DownloadNotificationHelperTest.kt
package studios.drible.tocabonito.core.downloads

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class DownloadNotificationHelperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val helper = DownloadNotificationHelper(context)

    @Test
    fun `createForegroundInfo returns valid ForegroundInfo`() {
        val workId = UUID.randomUUID()
        val info = helper.createForegroundInfo(workId, "Test Movie")

        assertNotNull(info)
        assertNotNull(info.notification)
    }

    @Test
    fun `notification channel is created`() {
        helper.ensureChannel()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
        val channel = manager.getNotificationChannel("tocabonito_downloads")
        assertNotNull(channel)
    }
}
```

- [ ] **GREEN:**

```kotlin
// core/downloads/src/main/kotlin/.../DownloadNotificationHelper.kt
package studios.drible.tocabonito.core.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import java.util.UUID
import javax.inject.Inject

class DownloadNotificationHelper @Inject constructor(
    private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "tocabonito_downloads"
        private const val CHANNEL_NAME = "Downloads"
        private const val NOTIFICATION_ID_BASE = 5000
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createForegroundInfo(workId: UUID, title: String): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID_BASE + workId.hashCode().and(0xFFF),
            notification,
        )
    }
}
```

- [ ] **REFACTOR:** None needed.

**Commit:** `feat(downloads): add DownloadNotificationHelper with channel setup`

---

## Test Fakes

```kotlin
// feature/detail/src/test/kotlin/.../FakeDownloadRepository.kt
package studios.drible.tocabonito.feature.detail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.repository.DownloadRepository

class FakeDownloadRepository : DownloadRepository {
    val savedItems = mutableListOf<DownloadItem>()
    val stateUpdates = mutableMapOf<String, DownloadState>()
    val progressUpdates = mutableMapOf<String, MutableList<Long>>()
    val pathUpdates = mutableMapOf<String, String>()

    private val flow = MutableStateFlow<List<DownloadItem>>(emptyList())

    override fun observeAll(): Flow<List<DownloadItem>> = flow
    override fun observeById(id: String): Flow<DownloadItem?> = MutableStateFlow(
        savedItems.find { it.id == id }
    )

    suspend fun getById(id: String): DownloadItem? = savedItems.find { it.id == id }

    override suspend fun save(item: DownloadItem) { savedItems.add(item) }
    override suspend fun updateState(id: String, state: DownloadState) {
        stateUpdates[id] = state
    }
    override suspend fun updateProgress(id: String, downloadedBytes: Long) {
        progressUpdates.getOrPut(id) { mutableListOf() }.add(downloadedBytes)
    }
    override suspend fun updateLocalPath(id: String, path: String) {
        pathUpdates[id] = path
    }
    override suspend fun delete(id: String) {
        savedItems.removeAll { it.id == id }
    }
}

// feature/detail/src/test/kotlin/.../FakeDownloadEnqueuer.kt
package studios.drible.tocabonito.feature.detail

import studios.drible.tocabonito.core.downloads.DownloadEnqueuer

class FakeDownloadEnqueuer : DownloadEnqueuer {
    val enqueuedIds = mutableListOf<String>()
    override suspend fun enqueue(downloadId: String) { enqueuedIds.add(downloadId) }
}

// core/downloads/src/test/kotlin/.../FakeHttpDownloader.kt
package studios.drible.tocabonito.core.downloads

import java.io.File
import java.io.IOException

class FakeHttpDownloader : HttpDownloader {
    var bytesToServe: Long = 0L
    var shouldThrow: Boolean = false

    override suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (bytesDownloaded: Long) -> Unit,
    ) {
        if (shouldThrow) throw IOException("Simulated failure")

        var written = 0L
        val chunkSize = 65_536L
        while (written < bytesToServe) {
            val chunk = minOf(chunkSize, bytesToServe - written)
            written += chunk
            onProgress(written)
        }
        destination.writeBytes(ByteArray(0)) // placeholder
    }
}
```

---

## DI Wiring (after all tests pass)

```kotlin
// core/downloads/src/main/kotlin/.../di/DownloadsModule.kt
package studios.drible.tocabonito.core.downloads.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.work.WorkManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import studios.drible.tocabonito.core.downloads.DefaultDownloadEnqueuer
import studios.drible.tocabonito.core.downloads.DownloadEnqueuer
import studios.drible.tocabonito.core.downloads.DownloadNotificationHelper
import studios.drible.tocabonito.core.downloads.HttpDownloader
import studios.drible.tocabonito.core.downloads.OkHttpDownloader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadsModule {

    @Binds
    abstract fun bindDownloadEnqueuer(impl: DefaultDownloadEnqueuer): DownloadEnqueuer

    @Binds
    abstract fun bindHttpDownloader(impl: OkHttpDownloader): HttpDownloader

    companion object {
        @Provides
        @Singleton
        fun provideDownloadNotificationHelper(
            @ApplicationContext context: Context,
        ): DownloadNotificationHelper = DownloadNotificationHelper(context)
    }
}
```

**Commit:** `feat(downloads): wire DI module for download components`

---

## Execution Order

1. Step 1 (ViewModel small file) — RED/GREEN/REFACTOR
2. Step 2 (ViewModel large file gate) — RED/GREEN/REFACTOR
3. Step 3 (ViewModel failure) — RED/GREEN/REFACTOR
4. Step 4 (DownloadEnqueuer) — RED/GREEN/REFACTOR
5. Step 5 (DownloadWorker) — RED/GREEN/REFACTOR
6. Step 6 (NotificationHelper) — RED/GREEN/REFACTOR
7. DI wiring — integration verify

---

## Verification

```bash
./gradlew :feature:detail:testDebugUnitTest
./gradlew :core:downloads:testDebugUnitTest
```

All tests green before moving to the next step. Each step gets its own commit.
