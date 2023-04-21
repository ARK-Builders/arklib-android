package space.taran.arklib.domain.preview.generator

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.taran.arklib.PreviewQuality
import space.taran.arklib.domain.meta.Kind
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.preview.Preview
import space.taran.arklib.domain.preview.PreviewGenerator
import space.taran.arklib.pdfPreviewGenerate
import java.nio.file.Path

object PdfPreviewGenerator : PreviewGenerator {

    override fun isValid(path: Path, meta: Metadata): Boolean {
        if (meta.kind != Kind.DOCUMENT) {
            return false
        }

        return (meta as Metadata.Document).isPdf
    }

    private val mutex = Mutex()

    override suspend fun generate(path: Path, meta: Metadata): Result<Preview> {
        // PDF preview generation must be sequential because Rust pdfium-render isn't thread-safe
        // See https://github.com/ARK-Builders/ARK-Navigator/pull/271
        val bitmap = mutex.withLock {
            pdfPreviewGenerate(path.toString(), PreviewQuality.MEDIUM)
        }

        return Result.success(Preview(bitmap, onlyThumbnail = false))
    }
}
