package dev.arkbuilders.arklib

import java.nio.file.Paths

class ArkFiles() {
    val data = folderConstants()
    val arkFolder = Paths.get(data["ARK_FOLDER"]!!)!!
    val statsFolder = Paths.get(data["STATS_FOLDER"]!!)!!
    val favoritesFile = Paths.get(data["FAVORITES_FILE"]!!)!!
    val tagStorageFile = Paths.get(data["TAG_STORAGE_FILE"]!!)!!
    val propertiesStorageFolder = Paths.get(data["PROPERTIES_STORAGE_FOLDER"]!!)!!
    val indexPath = Paths.get(data["INDEX_PATH"]!!)!!
    val metadataStorageFolder = Paths.get(data["METADATA_STORAGE_FOLDER"]!!)!!
    val previewStorageFolder = Paths.get(data["PREVIEWS_STORAGE_FOLDER"]!!)!!
    val thumbnailsStorageFolder = Paths.get(data["THUMBNAILS_STORAGE_FOLDER"]!!)!!
    val scoreStorageFile = Paths.get(data["SCORE_STORAGE_FILE"]!!)!!

    private external fun folderConstants(): Map<String, String>
}



val data = ArkFiles();
fun arkFolder() = data.arkFolder
fun arkStats() = data.statsFolder
fun arkFavorites() = data.favoritesFile
fun indexPath() = data.indexPath

// User-defined data
fun arkTags() = data.tagStorageFile
fun arkScores() = data.scoreStorageFile
fun arkProperties() = data.propertiesStorageFolder

// Generated data
fun arkMetadata() = data.metadataStorageFolder
fun arkPreviews() = data.previewStorageFolder
fun arkThumbnails() = data.thumbnailsStorageFolder
