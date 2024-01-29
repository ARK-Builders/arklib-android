package dev.arkbuilders.arklib.data.metadata.extractor

import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.metadata.Metadata
import dev.arkbuilders.arklib.data.metadata.MetadataExtractor
import java.nio.file.Path

object ImageMetadataExtractor: MetadataExtractor {

    override val acceptedExtensions: Set<String>
        get() = setOf("bmp", "gif", "ico", "jpg", "jpeg",
            "png", "tif", "tiff",
            "webp", "heic", "heif",
            "avif", "svg")

    override val acceptedMimeTypes: Set<String>
        get() = setOf(
            "image/bmp",
            "image/gif",
            "image/vnd.microsoft.icon",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/tiff",
            "image/webp",
            "image/heic",
            "image/avif",
            "image/svg+xml"
        )

    override fun extract(path: Path, resource: Resource): Result<Metadata> =
        Result.success(Metadata.Image())
}
