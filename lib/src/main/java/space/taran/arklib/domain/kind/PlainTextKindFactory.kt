package space.taran.arklib.domain.kind

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.meta.MetadataStorage
import java.nio.file.Path

object PlainTextKindFactory : ResourceKindFactory<ResourceKind.PlainText> {
    override val acceptedExtensions: Set<String> =
        setOf("txt")
    override val acceptedMimeTypes: Set<String>
        get() = setOf("text/plain")
    override val acceptedKindCode = KindCode.PLAINTEXT

    override fun fromPath(
        path: Path,
        meta: ResourceMeta,
    ) = ResourceKind.PlainText()
}
