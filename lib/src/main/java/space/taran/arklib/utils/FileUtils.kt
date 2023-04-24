package space.taran.arklib.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries

suspend fun listAllFiles(folder: Path): List<Path> =
    withContext(Dispatchers.IO) {
        val (directories, files) = listChildren(folder)

        return@withContext files + directories.flatMap {
            listAllFiles(it)
        }
    }

suspend fun deleteRecursively(folder: Path) =
    withContext(Dispatchers.IO) {
        folder.toFile()
            .walk(FileWalkDirection.BOTTOM_UP)
            .let { walk ->
                walk.forEach {
                    Files.delete(it.toPath())
                }
            }
    }

fun listChildren(folder: Path): Pair<List<Path>, List<Path>> = folder
    .listDirectoryEntries()
    .filter { !Files.isHidden(it) }
    .partition { Files.isDirectory(it) }

fun extension(path: Path): String {
    return path.extension.lowercase()
}

fun detectMimeType(path: Path): String? {
    return Tika().detect(Files.newInputStream(path))
}