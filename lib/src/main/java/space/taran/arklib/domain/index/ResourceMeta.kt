package space.taran.arklib.domain.index

import java.nio.file.attribute.FileTime

typealias ResourceIdLegacy = Long

data class ResourceMeta(
    val id: ResourceIdLegacy,
    val name: String,
    val extension: String,
    val modified: FileTime,
    val size: Long,
    var kind: ResourceKind?,
)