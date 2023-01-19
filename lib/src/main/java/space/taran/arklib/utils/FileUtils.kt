package space.taran.arklib.utils

import org.apache.tika.Tika
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries

fun listChildren(folder: Path): Pair<List<Path>, List<Path>> = folder
    .listDirectoryEntries()
    .filter { !Files.isHidden(it) }
    .partition { Files.isDirectory(it) }

fun extension(path: Path): String {
    return path.extension.lowercase()
}

fun getMimeTypeUsingTika(path: Path): String? {
    return Tika().detect(Files.newInputStream(path))
}
