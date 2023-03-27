package space.taran.arklib.domain.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

class UpdatedResourcesId(
    val deleted: Set<ResourceId>,
    val added: Map<ResourceId, Path>
)

class UpdatedResources(
    val deleted: Set<ResourceId>,
    val added: Map<ResourceMeta, Path>
)

@OptIn(ExperimentalPathApi::class)
// The index must read from the DAO only during application startup,
// since DB doesn't change from outside. But we must persist all changes
// during application lifecycle into the DAO for the case of any unexpected exit.
class PlainResourcesIndex internal constructor(
    private val root: Path,
    private val metadataStorage: MetadataStorage,
    private val messageFlow: MutableSharedFlow<Message>,
    private var nativeIndexBuilt: Boolean,
    resources: Map<Path, ResourceMeta>
) : ResourcesIndex {

    private val mutex = Mutex()
    private val mutUpdatedResourcesFlow = MutableSharedFlow<UpdatedResources>()

    val updatedResourcesFlow = mutUpdatedResourcesFlow.asSharedFlow()

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
            val update = if (nativeIndexBuilt)
                BindingIndex.update(root)
            else {
                BindingIndex.build(root)
                val added = BindingIndex.id2Path(root)
                nativeIndexBuilt = true
                UpdatedResourcesId(deleted = emptySet(), added)
            }
            handleUpdate(update)

            BindingIndex.store(root)
        }

    private suspend fun handleUpdate(update: UpdatedResourcesId) {
        update.deleted.forEach { id ->
            val path = pathById.remove(id)
            metaByPath.remove(path)
        }

        val addedMeta = update.added.mapNotNull { (id, path) ->
            val result = ResourceMeta.fromPath(id, path, metadataStorage)
            result.onFailure {
                messageFlow.emit(Message.KindDetectFailed(path))
                Log.d(
                    RESOURCES_INDEX,
                    "Could not detect kind for " +
                            path.absolutePathString()
                )
            }
            result.onSuccess { meta ->
                metaByPath[path] = meta
                pathById[id] = path
                return@mapNotNull meta to path
            }

            null
        }.toMap()

        mutUpdatedResourcesFlow.emit(UpdatedResources(update.deleted, addedMeta))
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
}
