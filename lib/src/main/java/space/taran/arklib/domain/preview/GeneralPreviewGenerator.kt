package space.taran.arklib.domain.preview

import android.util.Log
import space.taran.arklib.domain.preview.generator.ImagePreviewGenerator
import space.taran.arklib.utils.LogTags.PREVIEWS
import space.taran.arklib.utils.extension
import space.taran.arklib.utils.getMimeTypeUsingTika
import space.taran.arklib.domain.preview.generator.PdfPreviewGenerator
import space.taran.arklib.domain.preview.generator.PreviewGenerator
import space.taran.arklib.domain.preview.generator.TxtPreviewGenerator
import java.nio.file.Path
import kotlin.system.measureTimeMillis
import space.taran.arklib.domain.preview.generator.VideoPreviewGenerator

object GeneralPreviewGenerator {

    // Use this list to declare new types of generators
    private val generators = listOf(
        ImagePreviewGenerator,
        PdfPreviewGenerator,
        TxtPreviewGenerator,
        VideoPreviewGenerator
    )

    suspend fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val generator = findGenerator(path) ?: let {
            Log.d(PREVIEWS, "No generators found for $path")
            return
        }
        val time = measureTimeMillis {
            generator.generate(path, previewPath, thumbnailPath)
        }
        Log.d(PREVIEWS, "Preview and thumbnail generated for $path in $time ms")
    }

    private fun findGenerator(path: Path): PreviewGenerator? {
        var generator = generators.find { it.isValid(path) }
        if (generator != null) return generator

        if (extension(path).isNotEmpty()) return null
        val mimeType = getMimeTypeUsingTika(path) ?: return null
        Log.d(
            PREVIEWS,
            "GetFileTypeUsingTika $mimeType"
        )
        generator = generators.find { it.isValid(mimeType) }

        return generator
    }
}
