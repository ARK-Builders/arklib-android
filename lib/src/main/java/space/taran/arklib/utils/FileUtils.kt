package space.taran.arklib.utils

import android.util.Log
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

fun detectMimeType(path: Path): String? {
    Log.d(LogTags.TIKA, "invoking Apache Tika to detect MIME type")
    val mime = Tika().detect(Files.newInputStream(path))

    if (mime == null) {
        Log.w(LogTags.TIKA, "can't detect MIME type for $path")
    } else {
        Log.d(LogTags.TIKA, "$path is detected as $mime")
    }

    return mime
}
