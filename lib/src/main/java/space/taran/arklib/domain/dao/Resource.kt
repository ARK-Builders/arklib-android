package space.taran.arklib.domain.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import java.nio.file.Path

typealias Milliseconds = Long
typealias StringPath = String

@Entity
data class Resource(
    @PrimaryKey(autoGenerate = false)
    val id: ResourceId,
    val root: StringPath,
    val path: StringPath,
    val name: String,
    val extension: String,
    val modified: Milliseconds,
    val size: Long,
    val kind: Int?,
) {
    companion object {
        fun fromMeta(meta: ResourceMeta, root: Path, path: Path): Resource =
            Resource(
                id = meta.id,
                root = root.toString(),
                path = path.toString(),
                name = meta.name,
                extension = meta.extension,
                modified = meta.modified.toMillis(),
                size = meta.size,
                kind = meta.kind?.code?.ordinal
            )
    }
}
