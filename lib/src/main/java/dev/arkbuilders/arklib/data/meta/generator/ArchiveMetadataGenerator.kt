package dev.arkbuilders.arklib.data.meta.generator

import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.meta.MetadataGenerator
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
