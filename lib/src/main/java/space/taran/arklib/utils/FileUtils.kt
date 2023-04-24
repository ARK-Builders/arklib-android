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
    Log.d(LOG_PREFIX, "invoking Apache Tika to detect MIME type")
    val mime = Tika().detect(Files.newInputStream(path))

    if (mime == null) {
        Log.w(LOG_PREFIX, "can't detect MIME type for $path")
    } else {
        Log.d(LOG_PREFIX, "$path is detected as $mime")
    }

    return mime
}

private const val LOG_PREFIX: String = "[files]"