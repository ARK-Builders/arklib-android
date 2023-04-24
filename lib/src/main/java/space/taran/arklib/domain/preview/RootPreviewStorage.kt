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
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.meta.Kind
import space.taran.arklib.domain.meta.MetadataUpdate
import space.taran.arklib.domain.meta.RootMetadataStorage
import java.nio.file.Path
import kotlin.io.path.*

class RootPreviewStorage(
    private val scope: CoroutineScope,
    private val index: RootIndex,
    private val metadataStorage: RootMetadataStorage,
): PreviewStorage {

    val root = index.path

    private val previewsDir = root.arkFolder().arkPreviews()
    private val thumbnailsDir = root.arkFolder().arkThumbnails()

    private val _inProgress = MutableStateFlow(false)

    init {
        Log.d(LOG_PREFIX, "Initializing previews storage for root $root")
        previewsDir.createDirectories()
        thumbnailsDir.createDirectories()
        initUpdatedResourcesListener()

        // in contrast to MetadataStorage,
        // existing items are not retrieved from underlying layer,
        // existing metadata is pushed into `updates`
        // and should be processed in update handler here
    }

    override val inProgress = _inProgress.asStateFlow()

    override suspend fun locate(path: Path, id: ResourceId): Result<PreviewLocator> =
        locate(id)

    override suspend fun forget(id: ResourceId) {
        locate(id).getOrThrow().erase()
    }

    // always successful, if corresponding metadata exists
    // `result.status` can be `ABSENT` and should be checked by consumer
    private suspend fun locate(id: ResourceId): Result<PreviewLocator> {
        return metadataStorage
            .locate(id)
            .map {
                val locator = if (it.kind == Kind.IMAGE) {
                    val image: Path = index.getPath(id)!!
                    PreviewLocator(root, id, image)
                } else {
                    PreviewLocator(root, id)
                }

                return Result.success(locator)
            }
    }

    private fun generate(update: MetadataUpdate.Added) {
        scope.launch(Dispatchers.IO) {
            _inProgress.emit(true)

            launch {
                val id = update.id
                val locator = PreviewLocator(root, id)

                if (locator.status == PreviewStatus.ABSENT) {
                    Log.d(LOG_PREFIX, "Generating preview for $id")

                    val path = update.path
                    PreviewGenerator.generate(path, update.metadata)
                        .onSuccess { locator.store(it) }
                        .onFailure {
                            Log.w(LOG_PREFIX, "Failed to generate preview for $path")
                            Log.w(LOG_PREFIX, it.toString())
                        }
                }
            }.join()

            _inProgress.emit(false)
        }
    }

    private fun initUpdatedResourcesListener() {
        metadataStorage.updates.onEach { update ->
            when (update) {
                is MetadataUpdate.Added -> generate(update)
                is MetadataUpdate.Deleted -> forget(update.id)
            }
        }.launchIn(scope + Dispatchers.IO)
    }
}