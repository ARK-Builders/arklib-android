package space.taran.arklib.kind

import android.app.Application
import space.taran.arklib.ResourceId
import space.taran.arklib.index.ResourceMeta
import space.taran.arklib.meta.MetadataStorage
import java.nio.file.Path

object ArchiveKindFactory : ResourceKindFactory<ResourceKind.Archive> {
    override val acceptedExtensions: Set<String> =
        setOf("zip", "7z", "rar", "tar.gz", "tar.xz")

    override val acceptedMimeTypes: Set<String>
        get() = setOf("application/zip")
    override val acceptedKindCode = KindCode.ARCHIVE

    override fun fromPath(
        path: Path,
        meta: ResourceMeta,
        metadataStorage: MetadataStorage,
        app: Application?
    ) = ResourceKind.Archive()

    override fun fromRoom(extras: Map<MetaExtraTag, String>) =
        ResourceKind.Archive()

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Archive
    ): Map<MetaExtraTag, String?> =
        emptyMap()
}
