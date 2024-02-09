package dev.arkbuilders.arklib.data.metadata.extractor

import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.metadata.Metadata
import dev.arkbuilders.arklib.data.metadata.MetadataExtractor
import java.nio.file.Path

object LinkMetadataExtractor: MetadataExtractor {

    override val acceptedExtensions: Set<String>
        get() = setOf("link")

    override val acceptedMimeTypes: Set<String>
        get() = setOf()

    override fun extract(path: Path, resource: Resource): Result<Metadata> =
        Result.success(Metadata.Link())
}
