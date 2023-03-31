package space.taran.arklib.domain.kind

import space.taran.arklib.domain.index.Resource
import java.nio.file.Path

object ArchiveMetadataFactory : MetadataFactory<Metadata.Archive> {
    override val acceptedExtensions: Set<String> =
        setOf("zip", "7z", "rar", "tar.gz", "tar.xz")

    override val acceptedMimeTypes: Set<String>
        get() = setOf("application/zip")
    override val acceptedKindCode = KindCode.ARCHIVE

    override fun compute(
        path: Path,
        resource: Resource,
    ) = Metadata.Archive()
}
