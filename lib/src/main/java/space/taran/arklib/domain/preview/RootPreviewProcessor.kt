package space.taran.arklib.domain.preview

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import space.taran.arklib.*
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.meta.*
import space.taran.arklib.domain.processor.RootProcessor
import java.nio.file.Path

class RootPreviewProcessor private constructor(
    private val scope: CoroutineScope,
    private val index: RootIndex,
    private val metadata: RootMetadataProcessor,
) : RootProcessor<PreviewLocator, Unit>() {
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

    override suspend fun init() {
        Log.i(LOG_PREFIX, "Initializing previews storage for root $root")
        previews.init()
        thumbnails.init()
        initUpdatedResourcesListener()

        // in contrast to MetadataStorage,
        // existing items are not retrieved from underlying layer,
        // existing metadata is pushed into `updates`
        // and should be processed in update handler here
        scope.launch(Dispatchers.Default) {
            initKnownResources()
        }
    }

    override fun retrieve(id: ResourceId): Result<PreviewLocator> {
        val meta =  metadata.retrieve(id).getOrNull()

        meta?.let {
            if (meta.kind == Kind.IMAGE) {
                return Result.success(
                    PreviewLocator(root, id, images[id])
                )
            }
        }

        return Result.success(PreviewLocator(root, id))
    }

    override fun forget(id: ResourceId) {
        previews.remove(id)
        thumbnails.remove(id)
        images.remove(id)
    }

    private suspend fun generate(id: ResourceId, path: Path, metadata: Metadata) {
        val locator = PreviewLocator(root, id)

        if (metadata.kind == Kind.IMAGE) {
            images[id] = path
        }

        if (locator.status != PreviewStatus.ABSENT) {
            return
        }

        Log.d(LOG_PREFIX, "generating preview for $id")

        PreviewGenerator.generate(path, metadata)
            .onSuccess {
                if (it.onlyThumbnail) {
                    // images and text resources must fall into this branch
                    thumbnails.setValue(id, it.bitmap)

                    if (metadata.kind == Kind.IMAGE) {
                        images[id] = path
                    }
                    return
                }

                if (metadata.kind == Kind.IMAGE) {
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

    private fun initUpdatedResourcesListener() {
        metadata.updates.onEach { update ->
            _busy.emit(true)

            update.added.forEach { added ->
                generate(added.id, added.path, added.metadata)
            }
            previews.persist()
            thumbnails.persist()
            _busy.emit(false)

            update.deleted.forEach { deleted ->
                forget(deleted.id)
            }
        }.launchIn(scope + Dispatchers.Default)
    }

    private suspend fun initKnownResources() {
        _busy.emit(true)
        metadata.state().forEach { (id, meta) ->
            val path = index.getPath(id)!!
            generate(id, path, meta)
        }
        previews.persist()
        thumbnails.persist()
        _busy.emit(false)
    }

    companion object {
        suspend fun provide(
            scope: CoroutineScope,
            index: RootIndex,
            metadata: RootMetadataProcessor
        ) = RootPreviewProcessor(scope, index, metadata).also {
            it.init()
        }
    }
}