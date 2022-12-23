package space.taran.arklib.index

import space.taran.arklib.ResourceId
import space.taran.arklib.computeId
import space.taran.arklib.dao.ResourceWithExtra
import space.taran.arklib.kind.GeneralKindFactory
import space.taran.arklib.kind.ResourceKind
import space.taran.arklib.meta.MetadataStorage
import space.taran.arklib.utils.MetaResult
import space.taran.arklib.utils.extension
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

data class ResourceMeta(
    val id: ResourceId,
    val name: String,
    val extension: String,
    val modified: FileTime,
    val size: Long,
    var kind: ResourceKind?,
) {
    companion object {

        fun fromPath(path: Path, metadataStorage: MetadataStorage): MetaResult {
            val size = Files.size(path)
            if (size < 1) {
                return MetaResult.failure(IOException("Invalid file size"))
            }

            val id = computeId(size, path)

            val meta = ResourceMeta(
                id = id,
                name = path.fileName.toString(),
                extension = extension(path),
                modified = Files.getLastModifiedTime(path),
                size = size,
                kind = null
            )

            var kindDetectException: Exception? = null

            try {
                meta.kind = GeneralKindFactory.fromPath(path, meta, metadataStorage)
            } catch (e: Exception) {
                kindDetectException = e
            }
            return MetaResult(meta, kindDetectException)
        }

        fun fromRoom(room: ResourceWithExtra): ResourceMeta =
            ResourceMeta(
                id = room.resource.id,
                name = room.resource.name,
                extension = room.resource.extension,
                modified = FileTime.fromMillis(room.resource.modified),
                size = room.resource.size,
                kind = GeneralKindFactory.fromRoom(room.resource.kind, room.extras)
            )
    }
}