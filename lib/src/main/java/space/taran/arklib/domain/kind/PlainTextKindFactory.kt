package space.taran.arklib.domain.kind

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.KindCode
import space.taran.arklib.domain.index.MetaExtraTag
import space.taran.arklib.domain.index.ResourceKind
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
        metadataStorage: MetadataStorage
    ) = ResourceKind.PlainText()

    override fun fromRoom(extras: Map<MetaExtraTag, String>) =
        ResourceKind.PlainText()

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.PlainText
    ): Map<MetaExtraTag, String?> =
        emptyMap()
}
