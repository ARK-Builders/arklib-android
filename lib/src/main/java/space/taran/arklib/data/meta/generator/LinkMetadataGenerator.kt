package dev.arkbuilders.arklib.data.meta.generator

import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.meta.MetadataGenerator
import java.nio.file.Path

object LinkMetadataGenerator: MetadataGenerator {

    override val acceptedExtensions: Set<String>
        get() = setOf("link")

    override val acceptedMimeTypes: Set<String>
        get() = setOf()

    override fun generate(path: Path, resource: Resource): Result<Metadata> =
        Result.success(Metadata.Link())
}
