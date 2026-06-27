package studios.drible.tocabonito.core.data.download

import java.io.File

object IntegrityVerifier {
    fun verify(filePath: String, expectedSize: Long): Result<Unit> {
        val file = File(filePath)
        if (!file.exists()) return Result.failure(IllegalStateException("File does not exist: $filePath"))
        val actualSize = file.length()
        if (actualSize != expectedSize) {
            return Result.failure(
                IllegalStateException("Size mismatch: expected $expectedSize bytes, got $actualSize bytes")
            )
        }
        return Result.success(Unit)
    }
}
