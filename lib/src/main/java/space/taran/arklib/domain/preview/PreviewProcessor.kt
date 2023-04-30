package space.taran.arklib.domain.preview

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import space.taran.arklib.*
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.meta.*
import space.taran.arklib.domain.processor.RootProcessor
import java.nio.file.Path

class RootPreviewProcessor(
    private val scope: CoroutineScope,
    private val index: RootIndex,
    private val metadata: RootMetadataProcessor,
): RootProcessor<PreviewLocator, Unit>() {
    val root = index.path

    private val previews = BitmapStorage(
        scope, root.arkFolder().arkPreviews(), "previews"
    )
    private val thumbnails = BitmapStorage(
        scope, root.arkFolder().arkThumbnails(), "thumbnails"
    )

    // we fill it once during initialization to avoid
    // asking it every time from mutex-protected index
    private val images = mutableMapOf<ResourceId, Path>()

    init {
        Log.i(LOG_PREFIX, "Initializing previews storage for root $root")

        previews.refresh()
        thumbnails.refresh()

        // in contrast to MetadataStorage,
        // existing items are not retrieved from underlying layer,
        // existing metadata is pushed into `updates`
        // and should be processed in update handler here
        initUpdatedResourcesListener()
    }

    override fun forget(id: ResourceId) {
        previews.remove(id)
        thumbnails.remove(id)
    }

    // can be `Result.failure` only if the corresponding metadata doesn't exist
    // `locator.status` can be `ABSENT` and should be checked by consumer
    override fun retrieve(id: ResourceId): Result<PreviewLocator> = metadata
        .retrieve(id)
        .map { metadata ->
            val locator = if (metadata.kind == Kind.IMAGE) {
                PreviewLocator(root, id, images[id])
            } else {
                PreviewLocator(root, id)
            }

            return Result.success(locator)
        }

    private suspend fun generate(update: MetadataUpdate.Added) {
        val id = update.id
        val locator = PreviewLocator(root, id)

        if (locator.status == PreviewStatus.ABSENT) {
            Log.d(LOG_PREFIX, "generating preview for $id")

            val path = update.path
            PreviewGenerator.generate(path, update.metadata)
                .onSuccess {
                    if (it.onlyThumbnail) {
                        // images and text resources must fall into this branch
                        thumbnails.setValue(id, it.bitmap)

                        if (update.metadata.kind == Kind.IMAGE) {
                            images[id] = update.path
                        }
                        return
                    }

                    if (update.metadata.kind == Kind.IMAGE) {
                        throw IllegalStateException("Images have only thumbnail")
                    }

                    previews.setValue(id, it.bitmap)

                    val thumbnail = Preview.downscale(it.bitmap)
                    thumbnails.setValue(id, thumbnail)
                }
                .onFailure {
                    Log.w(LOG_PREFIX, "Failed to generate preview for $path")
                    Log.w(LOG_PREFIX, it.toString())
                }
        }
    }

    private fun initUpdatedResourcesListener() {
        scope.launch(Dispatchers.IO) {
            _busy.emit(true)

            metadata.updates.onEach { update ->
                when (update) {
                    is MetadataUpdate.Added -> generate(update)
                    is MetadataUpdate.Deleted -> forget(update.id)
                }
            }

            _busy.emit(false)
        }
    }
}