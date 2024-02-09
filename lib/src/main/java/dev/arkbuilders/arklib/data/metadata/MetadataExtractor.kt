package dev.arkbuilders.arklib.data.metadata

import android.util.Log
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.metadata.extractor.*
import dev.arkbuilders.arklib.utils.detectMimeType
import dev.arkbuilders.arklib.utils.extension
import java.nio.file.Path
import kotlin.system.measureTimeMillis

interface MetadataExtractor {

    val acceptedExtensions: Set<String>

    // https://mimetype.io/
    val acceptedMimeTypes: Set<String>

    fun extract(path: Path, resource: Resource): Result<Metadata>

    fun isValid(path: Path): Boolean {
        if (acceptedExtensions.contains(extension(path))) {
            return true
        }

        val mimeType = detectMimeType(path) ?: return false
        return acceptedMimeTypes.contains(mimeType)
    }

    companion object {
        fun extract(path: Path, resource: Resource): Result<Metadata> {
            val generator = EXTRACTORS.find {
                it.isValid(path)
            } ?: let {
                Log.d(LOG_PREFIX, "No generators found for $path")
                return Result.success(Metadata.Unknown())
            }

            var result: Result<Metadata>
            val time = measureTimeMillis {
                result = generator.extract(path, resource)
            }
            Log.v(LOG_PREFIX, "metadata generated for $path in $time ms")
            return result
        }

        // Use this list to declare new types of generators
        private val EXTRACTORS: List<MetadataExtractor> = listOf(
            ImageMetadataExtractor,
            VideoMetadataExtractor,
            DocumentMetadataExtractor,
            PlainTextMetadataExtractor,
            ArchiveMetadataExtractor
        )
    }
}
