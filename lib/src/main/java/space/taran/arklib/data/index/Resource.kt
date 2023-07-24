package space.taran.arklib.data.index

import space.taran.arklib.ResourceId
import space.taran.arklib.utils.extension
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists

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
            if (!path.exists()) {
                return Result.failure(FileNotFoundException(path.toString()))
            }

            val size = Files.size(path)
            if (size < 1) {
                return Result.failure(IOException("Invalid size of a file $path"))
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
