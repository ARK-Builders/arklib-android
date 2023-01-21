package space.taran.arklib.domain.preview.generator

import android.graphics.Bitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.taran.arklib.PreviewQuality
import space.taran.arklib.pdfPreviewGenerate
import java.nio.file.Path

object PdfPreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions = setOf("pdf")
    override val acceptedMimeTypes = setOf("application/pdf")
    private val mutex = Mutex()

    override suspend fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        // PDF preview generation must be sequential because Rust pdfium-render isn't thread-safe
        // See https://github.com/ARK-Builders/ARK-Navigator/pull/271
        val preview = mutex.withLock {
            generatePreview(path)
        }
        storePreview(previewPath, preview)
        val thumbnail = resizePreviewToThumbnail(preview)
        storeThumbnail(thumbnailPath, thumbnail)
    }

    private fun generatePreview(source: Path): Bitmap {
        return pdfPreviewGenerate(source.toString(), PreviewQuality.MEDIUM)
    }
}
