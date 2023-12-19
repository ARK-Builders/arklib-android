package dev.arkbuilders.arklib

import dev.arkbuilders.arklib.ArkFiles.ARK_FOLDER
import dev.arkbuilders.arklib.ArkFiles.FAVORITES_FILE
import dev.arkbuilders.arklib.ArkFiles.METADATA_STORAGE_FOLDER
import dev.arkbuilders.arklib.ArkFiles.PREVIEWS_STORAGE_FOLDER
import dev.arkbuilders.arklib.ArkFiles.PROPERTIES_STORAGE_FOLDER
import dev.arkbuilders.arklib.ArkFiles.SCORE_STORAGE_FILE
import dev.arkbuilders.arklib.ArkFiles.STATS_FOLDER
import dev.arkbuilders.arklib.ArkFiles.TAG_STORAGE_FILE
import dev.arkbuilders.arklib.ArkFiles.THUMBNAILS_STORAGE_FOLDER
import java.nio.file.Path
import kotlin.io.path.Path

private class NativeArkFiles(
    val ARK_FOLDER: String,
    val STATS_FOLDER: String,
    val FAVORITES_FILE: String,
    val INDEX_PATH: String,
    val TAG_STORAGE_FILE: String,
    val SCORE_STORAGE_FILE: String,
    val PROPERTIES_STORAGE_FOLDER: String,
    val METADATA_STORAGE_FOLDER: String,
    val PREVIEWS_STORAGE_FOLDER: String,
    val THUMBNAILS_STORAGE_FOLDER: String,
)

object ArkFiles {
    private val nativeArkFiles by lazy {
        provideNativeArkFiles()
    }

    private external fun provideNativeArkFiles(): NativeArkFiles

    val ARK_FOLDER by lazy { Path(nativeArkFiles.ARK_FOLDER) }
    val STATS_FOLDER by lazy { Path(nativeArkFiles.STATS_FOLDER) }
    val FAVORITES_FILE by lazy { Path(nativeArkFiles.FAVORITES_FILE) }

    // User-defined data
    val TAG_STORAGE_FILE by lazy { Path(nativeArkFiles.TAG_STORAGE_FILE) }
    val SCORE_STORAGE_FILE by lazy { Path(nativeArkFiles.SCORE_STORAGE_FILE) }
    val PROPERTIES_STORAGE_FOLDER by lazy {
        Path(nativeArkFiles.PROPERTIES_STORAGE_FOLDER)
    }

    // Generated data
    val METADATA_STORAGE_FOLDER by lazy {
        Path(nativeArkFiles.METADATA_STORAGE_FOLDER)
    }
    val PREVIEWS_STORAGE_FOLDER by lazy {
        Path(nativeArkFiles.PREVIEWS_STORAGE_FOLDER)
    }
    val THUMBNAILS_STORAGE_FOLDER by lazy {
        Path(nativeArkFiles.THUMBNAILS_STORAGE_FOLDER)
    }

}

fun Path.arkFolder() = resolve(ARK_FOLDER)
fun Path.arkStats() = resolve(STATS_FOLDER)
fun Path.arkFavorites() = resolve(FAVORITES_FILE)

// User-defined data
fun Path.arkTags() = resolve(TAG_STORAGE_FILE)
fun Path.arkScores() = resolve(SCORE_STORAGE_FILE)
fun Path.arkProperties() = resolve(PROPERTIES_STORAGE_FOLDER)

// Generated data
fun Path.arkMetadata() = resolve(METADATA_STORAGE_FOLDER)
fun Path.arkPreviews() = resolve(PREVIEWS_STORAGE_FOLDER)
fun Path.arkThumbnails() = resolve(THUMBNAILS_STORAGE_FOLDER)
