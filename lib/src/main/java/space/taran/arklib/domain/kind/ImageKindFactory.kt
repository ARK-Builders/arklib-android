package space.taran.arklib.domain.kind

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.KindCode
import space.taran.arklib.domain.index.MetaExtraTag
import space.taran.arklib.domain.index.ResourceKind
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.meta.MetadataStorage
import java.nio.file.Path

object ImageKindFactory : ResourceKindFactory<ResourceKind.Image> {
    override val acceptedExtensions: Set<String> =
        setOf("jpg", "jpeg", "png", "svg", "gif", "webp")
    override val acceptedMimeTypes: Set<String>
        get() = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
        )

    override val acceptedKindCode = KindCode.IMAGE

    override fun fromPath(
        path: Path,
        meta: ResourceMeta,
        metadataStorage: MetadataStorage
    ) = ResourceKind.Image()

    override fun fromRoom(extras: Map<MetaExtraTag, String>) = ResourceKind.Image()

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Image
    ): Map<MetaExtraTag, String?> =
        emptyMap()
}
