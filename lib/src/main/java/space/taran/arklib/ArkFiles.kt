package dev.arkbuilders.arklib

import java.nio.file.Path


class ArkFiles() {
    val data = folderConstants()
    val arkFolder = data["ARK_FOLDER"]
    val statsFolder = data["STATS_FOLDER"]
    val favoritesFile = data["FAVORITES_FILE"]
    val tagStorageFile = data["TAG_STORAGE_FILE"]
    val propertiesStorageFolder = data["PROPERTIES_STORAGE_FOLDER"]
    val indexPath = data["INDEX_PATH"]
    val metadataStorageFolder = data["METADATA_STORAGE_FOLDER"]
    val previewStorageFolder = data["PREVIEWS_STORAGE_FOLDER"]
    val thumbnailsStorageFolder = data["THUMBNAILS_STORAGE_FOLDER"]
    val scoreStorageFile = data["SCORE_STORAGE_FILE"]

    private external fun folderConstants(): Map<String, String>
}



val data = ArkFiles();
fun Path.arkFolder() = resolve(data.arkFolder)
fun Path.arkStats() = resolve(data.statsFolder)
fun Path.arkFavorites() = resolve(data.favoritesFile)
fun Path.indexPath() = resolve(data.indexPath)

// User-defined data
fun Path.arkTags() = resolve(data.tagStorageFile)
fun Path.arkScores() = resolve(data.scoreStorageFile)
fun Path.arkProperties() = resolve(data.propertiesStorageFolder)

// Generated data
fun Path.arkMetadata() = resolve(data.metadataStorageFolder)
fun Path.arkPreviews() = resolve(data.previewStorageFolder)
fun Path.arkThumbnails() = resolve(data.thumbnailsStorageFolder)
