package space.taran.arklib.domain.meta.generator

import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.meta.MetadataGenerator
import java.nio.file.Path

object LinkMetadataGenerator: MetadataGenerator {

    override val acceptedExtensions: Set<String>
        get() = setOf("link")

    override val acceptedMimeTypes: Set<String>
        get() = setOf()

    override fun generate(path: Path, resource: Resource): Result<Metadata> =
        Result.success(Metadata.Link()) // todo: implement
}
