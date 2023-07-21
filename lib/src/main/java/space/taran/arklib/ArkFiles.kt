package space.taran.arklib

import space.taran.arklib.ArkFiles.ARK_FOLDER
import space.taran.arklib.ArkFiles.FAVORITES_FILE
import space.taran.arklib.ArkFiles.METADATA_STORAGE_FOLDER
import space.taran.arklib.ArkFiles.PREVIEWS_STORAGE_FOLDER
import space.taran.arklib.ArkFiles.PROPERTIES_STORAGE_FOLDER
import space.taran.arklib.ArkFiles.SCORE_STORAGE_FILE
import space.taran.arklib.ArkFiles.STATS_FOLDER
import space.taran.arklib.ArkFiles.TAG_STORAGE_FILE
import space.taran.arklib.ArkFiles.THUMBNAILS_STORAGE_FOLDER
import java.nio.file.Path

object ArkFiles {
    const val ARK_FOLDER = ".ark"
    const val STATS_FOLDER = "stats"
    const val FAVORITES_FILE = "favorites"

    // User-defined data
    const val TAG_STORAGE_FILE = "user/tags"
    const val SCORE_STORAGE_FILE = "user/scores"
    const val PROPERTIES_STORAGE_FOLDER = "user/properties"

    // Generated data
    const val METADATA_STORAGE_FOLDER = "cache/meta"
    const val PREVIEWS_STORAGE_FOLDER = "cache/previews"
    const val THUMBNAILS_STORAGE_FOLDER = "cache/thumbnails"
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
