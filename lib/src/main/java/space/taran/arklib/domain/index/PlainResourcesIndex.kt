package space.taran.arklib.domain.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arklib.ResourceId
import space.taran.arklib.binding.BindingIndex
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.meta.MetadataStorage
import space.taran.arklib.domain.preview.PreviewStorage
import space.taran.arklib.utils.LogTags.PREVIEWS
import space.taran.arklib.utils.LogTags.RESOURCES_INDEX
import space.taran.arklib.utils.withContextAndLock
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.system.measureTimeMillis

class UpdatedResources(
    val deleted: Set<ResourceId>,
    val added: Map<ResourceId, Path>
)

@OptIn(ExperimentalPathApi::class)
// The index must read from the DAO only during application startup,
// since DB doesn't change from outside. But we must persist all changes
// during application lifecycle into the DAO for the case of any unexpected exit.
class PlainResourcesIndex internal constructor(
    private val root: Path,
    private val previewStorage: PreviewStorage,
    private val metadataStorage: MetadataStorage,
    private val messageFlow: MutableSharedFlow<Message>,
    private var nativeIndexBuilded: Boolean,
    resources: Map<Path, ResourceMeta>
) : ResourcesIndex {

    private val mutex = Mutex()

    internal val metaByPath: MutableMap<Path, ResourceMeta> =
        resources.toMutableMap()

    private val pathById: MutableMap<ResourceId, Path> =
        resources.map { (path, meta) ->
            meta.id to path
        }
            .toMap()
            .toMutableMap()

    override suspend fun listResources(
        prefix: Path?
    ): Set<ResourceMeta> = mutex.withLock {
        val metas = if (prefix != null) {
            metaByPath.filterKeys { it.startsWith(prefix) }
        } else {
            metaByPath
        }.values

        Log.d(RESOURCES_INDEX, "${metas.size} resources returned")
        return metas.toSet()
    }

    fun contains(id: ResourceId) = pathById.containsKey(id)

    override suspend fun getPath(id: ResourceId): Path = mutex.withLock {
        tryGetPath(id)!!
    }

    override suspend fun getMeta(id: ResourceId): ResourceMeta = mutex.withLock {
        tryGetMeta(id)!!
    }

    override suspend fun remove(id: ResourceId): Path = mutex.withLock {
        Log.d(RESOURCES_INDEX, "forgetting resource $id")
        return tryRemove(id)!!
    }

    override suspend fun reindex(): Unit =
        withContextAndLock(Dispatchers.IO, mutex) {
            val update = if (nativeIndexBuilded)
                BindingIndex.update(root)
            else {
                BindingIndex.build(root)
                val added = BindingIndex.id2Path(root)
                nativeIndexBuilded = true
                UpdatedResources(deleted = emptySet(), added)
            }
            handleUpdate(update)

            BindingIndex.store(root)
        }

    private suspend fun handleUpdate(update: UpdatedResources) {
        update.deleted.forEach { id ->
            val path = pathById.remove(id)
            metaByPath.remove(path)
            previewStorage.forget(id)
        }

        update.added.forEach { (id, path) ->
            val result = ResourceMeta.fromPath(id, path, metadataStorage)
            result.onSuccess { meta ->
                metaByPath[path] = meta
                pathById[id] = path
            }
            result.onFailure {
                messageFlow.emit(Message.KindDetectFailed(path))
                Log.d(
                    RESOURCES_INDEX,
                    "Could not detect kind for " +
                            path.absolutePathString()
                )
            }

        }

        val time2 = measureTimeMillis {
            providePreviews()
        }
        Log.d(PREVIEWS, "previews provided in ${time2}ms")
    }

    // should be only used in AggregatedResourcesIndex
    fun tryGetPath(id: ResourceId): Path? = pathById[id]

    // should be only used in AggregatedResourcesIndex
    fun tryGetMeta(id: ResourceId): ResourceMeta? {
        val path = tryGetPath(id)
        if (path != null) {
            return metaByPath[path]
        }
        return null
    }

    // should be only used in AggregatedResourcesIndex
    fun tryRemove(id: ResourceId): Path? {
        val path = pathById.remove(id) ?: return null

        val idRemoved = metaByPath.remove(path)!!.id

        if (id != idRemoved) {
            throw AssertionError("internal mappings are diverged")
        }

        val duplicatedResource = metaByPath
            .entries
            .find { entry -> entry.value.id == idRemoved }
        duplicatedResource?.let { entry ->
            pathById[entry.value.id] = entry.key
        }

        return path
    }

    internal suspend fun providePreviews() =
        withContext(Dispatchers.IO) {
            Log.d(
                PREVIEWS,
                "providing previews/thumbnails for ${metaByPath.size} resources"
            )

            supervisorScope {
                metaByPath.entries.map { (path: Path, meta: ResourceMeta) ->
                    async(Dispatchers.IO) {
                        previewStorage.store(path, meta)
                    } to path
                }.forEach { (generateTask, path) ->
                    try {
                        generateTask.await()
                    } catch (e: Exception) {
                        Log.e(
                            PREVIEWS,
                            "Failed to generate preview/thumbnail for id ${
                                metaByPath[path]?.id
                            } ($path)"
                        )
                    }
                }
            }
        }

    // todo: update resource in native index
    override suspend fun updateResource(
        oldId: ResourceId,
        path: Path,
        newResource: ResourceMeta
    ) {
        metaByPath[path] = newResource
        pathById.remove(oldId)
        pathById[newResource.id] = path
    }
}
