package space.taran.arklib.domain.kind

import space.taran.arklib.domain.index.Resource
import java.nio.file.Path

object LinkMetadataFactory : MetadataFactory<Metadata.Link> {
    override val acceptedExtensions = setOf("link")
    override val acceptedKindCode = KindCode.LINK
    override val acceptedMimeTypes: Set<String>
        get() = setOf()

    override fun compute(
        path: Path,
        resource: Resource
    ): Metadata.Link {
        return Metadata.Link()
    }
}
