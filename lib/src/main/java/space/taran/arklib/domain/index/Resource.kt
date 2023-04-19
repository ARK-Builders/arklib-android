package space.taran.arklib.domain.index

import space.taran.arklib.ResourceId
import space.taran.arklib.utils.extension
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

data class Resource(
    val id: ResourceId,
    val name: String,
    val extension: String,
    val modified: FileTime,
) {

    fun size() = id.dataSize

    companion object {
        fun compute(
            id: ResourceId,
            path: Path
        ): Result<Resource> {
            val size = Files.size(path)
            if (size < 1) {
                return Result.failure(IOException("Invalid file size"))
            }

            return Result.success(Resource(
                id = id,
                name = path.fileName.toString(),
                extension = extension(path),
                modified = Files.getLastModifiedTime(path)
            ))
        }
    }
}
