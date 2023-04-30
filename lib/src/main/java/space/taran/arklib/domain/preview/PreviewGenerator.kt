package space.taran.arklib.domain.preview

import android.util.Log
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.preview.generator.ImagePreviewGenerator
import space.taran.arklib.domain.preview.generator.PdfPreviewGenerator
import space.taran.arklib.domain.preview.generator.TxtPreviewGenerator
import space.taran.arklib.domain.preview.generator.VideoPreviewGenerator
import java.nio.file.Path
import kotlin.system.measureTimeMillis

interface PreviewGenerator {

    fun isValid(path: Path, meta: Metadata): Boolean

    suspend fun generate(path: Path, meta: Metadata): Result<Preview>

    companion object {
        suspend fun generate(path: Path, meta: Metadata): Result<Preview> {
            val generator = GENERATORS.find {
                it.isValid(path, meta)
            } ?: let {
                return Result.failure(
                    IllegalArgumentException("No generators found for $path")
                )
            }

            var result: Result<Preview>?
            val time = measureTimeMillis {
                result = generator.generate(path, meta)
            }
            Log.d(LOG_PREFIX, "preview generated for $path in $time ms")
            return result!!
        }

        // Use this list to declare new types of generators
        private val GENERATORS: List<PreviewGenerator> = listOf(
            ImagePreviewGenerator,
            PdfPreviewGenerator,
            TxtPreviewGenerator,
            VideoPreviewGenerator
        )
    }
}
