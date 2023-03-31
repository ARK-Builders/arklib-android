package space.taran.arklib.domain.kind

import space.taran.arklib.domain.index.Resource
import java.nio.file.Path

object PlainTextMetadataFactory : MetadataFactory<Metadata.PlainText> {
    override val acceptedExtensions: Set<String> =
        setOf("txt")
    override val acceptedMimeTypes: Set<String>
        get() = setOf("text/plain")
    override val acceptedKindCode = KindCode.PLAINTEXT

    override fun compute(
        path: Path,
        resource: Resource,
    ) = Metadata.PlainText()
}
