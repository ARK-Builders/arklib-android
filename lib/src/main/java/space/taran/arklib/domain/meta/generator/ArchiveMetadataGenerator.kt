package space.taran.arklib.domain.meta.generator

import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.meta.MetadataGenerator
import java.nio.file.Path

object ArchiveMetadataGenerator: MetadataGenerator {

    override val acceptedExtensions: Set<String>
        get() = setOf("zip", "tar", "gz",
            "bz", "bz2", "jar", "rar",
            "tar.gz", "tar.xz", "7z")

    override val acceptedMimeTypes: Set<String>
        get() = setOf(
            "application/zip",
            "application/x-tar",
            "application/gzip",
            "application/x-bzip",
            "application/x-bzip2",
            "application/java-archive",
            "application/vnd.rar",
            "application/x-7z-compressed"
        )

    override fun generate(path: Path, resource: Resource): Result<Metadata> =
        Result.success(Metadata.Archive())
}
