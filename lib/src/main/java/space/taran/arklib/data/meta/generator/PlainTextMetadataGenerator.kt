package space.taran.arklib.data.meta.generator

import space.taran.arklib.data.index.Resource
import space.taran.arklib.data.meta.Metadata
import space.taran.arklib.data.meta.MetadataGenerator
import java.nio.file.Path

object PlainTextMetadataGenerator: MetadataGenerator {

    override val acceptedExtensions: Set<String>
        get() = setOf("txt", "log", "text",
            "pl", "list", "diff")

    override val acceptedMimeTypes: Set<String>
        get() = setOf("text/plain")

    override fun generate(path: Path, resource: Resource): Result<Metadata> =
        Result.success(Metadata.PlainText())
}
