package space.taran.arklib.domain.preview

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkPreviews
import space.taran.arklib.arkThumbnails
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.meta.MetadataUpdate
import space.taran.arklib.domain.meta.RootMetadataStorage
import space.taran.arklib.utils.LogTags.PREVIEWS
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.*

class RootPreviewStorage(
    val root: Path,
    private val metadataStorage: RootMetadataStorage,
    private val appScope: CoroutineScope
): PreviewStorage {

    private val previewsDir = root.arkFolder().arkPreviews()
    private val thumbnailsDir = root.arkFolder().arkThumbnails()

    private val _inProgress = MutableStateFlow(false)

    init {
        previewsDir.createDirectories()
        thumbnailsDir.createDirectories()
        initUpdatedResourcesListener()

        // in contrast to MetadataStorage,
        // existing items are not retrieved from underlying layer,
        // existing metadata is pushed into `updates`
        // and should be processed in update handler here
    }

    override val inProgress = _inProgress.asStateFlow()

    override fun locate(path: Path, id: ResourceId): Result<PreviewLocator> =
        locate(id)

    override fun forget(id: ResourceId) {
        locate(id).getOrThrow().erase()
    }

    private fun locate(id: ResourceId): Result<PreviewLocator> {
        val locator = PreviewLocator(root, id)
        if (locator.status == PreviewStatus.ABSENT) {
            return Result.failure(
                FileNotFoundException(locator.thumbnail().toString())
            )
        }

        return Result.success(locator)
    }

    private fun generate(update: MetadataUpdate.Added) {
        appScope.launch(Dispatchers.IO) {
            _inProgress.emit(true)

            launch {
                generate(update.id, update.path, update.metadata)
            }.join()

            _inProgress.emit(false)
        }
    }

    private suspend fun generate(id: ResourceId, path: Path, meta: Metadata) {
        val locator = PreviewLocator(root, id)
        if (locator.status != PreviewStatus.ABSENT) {
            return
        }

        PreviewGenerator.generate(path, meta)
            .onSuccess { locator.store(it) }
            .onFailure {
                Log.e(PREVIEWS, "Failed to generate preview for $path")
                Log.e(PREVIEWS, it.toString())
            }
    }

    private fun initUpdatedResourcesListener() {
        metadataStorage.updates.onEach { update ->
            when (update) {
                is MetadataUpdate.Added -> generate(update)
                is MetadataUpdate.Deleted -> forget(update.id)
            }
        }.launchIn(appScope + Dispatchers.IO)
    }
}
