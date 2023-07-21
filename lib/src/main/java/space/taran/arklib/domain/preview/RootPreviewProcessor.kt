package space.taran.arklib.domain.preview

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import space.taran.arklib.*
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.meta.*
import space.taran.arklib.domain.processor.RootProcessor
import space.taran.arklib.domain.storage.BitmapStorage
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class RootPreviewProcessor private constructor(
    private val scope: CoroutineScope,
    private val index: RootIndex,
    private val metadata: RootMetadataProcessor,
) : RootProcessor<PreviewLocator, Unit>() {
    val root = index.path

    private val previews = BitmapStorage(scope, root.arkFolder().arkPreviews())
    private val thumbnails = BitmapStorage(scope, root.arkFolder().arkThumbnails())

    private val generateJobs = ConcurrentHashMap<ResourceId, Job>()

    // we fill it once during initialization to avoid
    // asking it every time from mutex-protected index
    internal val images = ConcurrentHashMap<ResourceId, Path>()

    override suspend fun init() {
        Log.i(LOG_PREFIX, "Initializing previews storage for root $root")
        initUpdatedResourcesListener()

        // in contrast to MetadataStorage,
        // existing items are not retrieved from underlying layer,
        // existing metadata is pushed into `updates`
        // and should be processed in update handler here
        initKnownResources()
    }

    override fun retrieve(id: ResourceId): Result<PreviewLocator> {
        val meta = metadata.retrieve(id).getOrNull()

        meta?.let {
            if (meta.kind == Kind.IMAGE) {
                return Result.success(
                    PreviewLocator(
                        this,
                        root,
                        id,
                        generateJobs[id]
                    )
                )
            }
        }

        return Result.success(
            PreviewLocator(this, root, id, generateJobs[id])
        )
    }

    override fun forget(id: ResourceId) {
        previews.remove(id)
        thumbnails.remove(id)
        images.remove(id)
    }

    private suspend fun generate(
        id: ResourceId,
        path: Path,
        metadata: Metadata
    ) = scope.launch(Dispatchers.Default) {
        val locator = PreviewLocator(this@RootPreviewProcessor, root, id)

        if (metadata.kind == Kind.IMAGE) {
            images[id] = path
        }

        if (locator.status != PreviewStatus.ABSENT) {
            return@launch
        }

        Log.d(LOG_PREFIX, "generating preview for $id")

        PreviewGenerator.generate(path, metadata)
            .onSuccess {
                if (it.onlyThumbnail) {
                    // images and text resources must fall into this branch
                    thumbnails.saveBitmap(id, it.bitmap)

                    if (metadata.kind == Kind.IMAGE) {
                        images[id] = path
                    }
                    return@launch
                }

                if (metadata.kind == Kind.IMAGE) {
                    throw IllegalStateException("Images have only thumbnail")
                }

                previews.saveBitmap(id, it.bitmap)

                val thumbnail = Preview.downscale(it.bitmap)
                thumbnails.saveBitmap(id, thumbnail)
            }
            .onFailure {
                Log.w(LOG_PREFIX, "Failed to generate preview for $path")
                Log.w(LOG_PREFIX, it.toString())
            }

        generateJobs.remove(id)
    }.also { job ->
        generateJobs[id] = job
    }


    private fun initUpdatedResourcesListener() {
        metadata.updates.onEach { update ->
            _busy.emit(true)

            val jobs = update.added.map { added ->
                generate(added.id, added.path, added.metadata)
            }
            jobs.joinAll()

            _busy.emit(false)

            update.deleted.forEach { deleted ->
                forget(deleted.id)
            }
        }.launchIn(scope + Dispatchers.Default)
    }

    private suspend fun initKnownResources() {
        _busy.emit(true)
        val jobs = metadata.state().map { (id, meta) ->
            // Workaround
            // Right now we are not removing lost resource meta, which causes npe
            // Here should be index.getPath(id)!!, see:
            // https://github.com/ARK-Builders/arklib-android/issues/70
            index.getPath(id)?.let { path ->
                generate(id, path, meta)
            }
        }
        // UI is unlocked only after the generation of all images has been started
        scope.launch {
            jobs.filterNotNull().joinAll()
            _busy.emit(false)
        }
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