package dev.arkbuilders.arklib.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries

data class PartialResult<S, F>(val succeeded: S, val failed: F)

internal val ROOT_PATH: Path = Path("/")

internal val ANDROID_DIRECTORY: Path = Path("Android")

val INTERNAL_STORAGE = Path("/storage/emulated/0")

internal class DeviceStorageUtils(private val appContext: Context) {
    fun listStorages(): List<Path> =
        appContext.getExternalFilesDirs(null)
            .toList()
            .filterNotNull()
            .filter { it.exists() }
            .map {
                it.toPath().toRealPath()
                    .takeWhile { part ->
                        part != ANDROID_DIRECTORY
                    }
                    .fold(ROOT_PATH) { parent, child ->
                        parent.resolve(child)
                    }
            }
}

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
    return try {
        Tika().detect(Files.newInputStream(path))
    } catch (e: Throwable) {
        Log.d(
            "FileUtils",
            "Tika failed to detect mime type for $path because ${e.message}"
        )
        null
    }
}