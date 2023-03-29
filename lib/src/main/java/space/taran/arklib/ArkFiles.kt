package space.taran.arklib

import space.taran.arklib.ArkFiles.ARK_FOLDER
import space.taran.arklib.ArkFiles.FAVORITES_FILE
import space.taran.arklib.ArkFiles.METADATA_FOLDER
import space.taran.arklib.ArkFiles.PREVIEWS_FOLDER
import space.taran.arklib.ArkFiles.SCORES_STORAGE_FILE
import space.taran.arklib.ArkFiles.STATS_FOLDER
import space.taran.arklib.ArkFiles.TAGS_STORAGE_FILE
import space.taran.arklib.ArkFiles.THUMBNAILS_FOLDER
import java.nio.file.Path

object ArkFiles {
    const val ARK_FOLDER = ".ark"
    const val STATS_FOLDER = "stats"
    const val FAVORITES_FILE = "favorites"
    const val TAGS_STORAGE_FILE = "tags"
    const val PREVIEWS_FOLDER = "previews"
    const val METADATA_FOLDER = "meta"
    const val THUMBNAILS_FOLDER = "thumbnails"
    const val SCORES_STORAGE_FILE = "scores"
}

fun Path.arkFolder() = resolve(ARK_FOLDER)
fun Path.arkStats() = resolve(STATS_FOLDER)
fun Path.arkFavorites() = resolve(FAVORITES_FILE)
fun Path.arkTags() = resolve(TAGS_STORAGE_FILE)
fun Path.arkPreviews() = resolve(PREVIEWS_FOLDER)
fun Path.arkThumbnails() = resolve(THUMBNAILS_FOLDER)
fun Path.arkMetadata() = resolve(METADATA_FOLDER)
fun Path.arkScores() = resolve(SCORES_STORAGE_FILE)
