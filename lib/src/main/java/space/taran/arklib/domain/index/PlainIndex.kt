package space.taran.arklib.domain.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.taran.arklib.ResourceId
import space.taran.arklib.binding.BindingIndex
import space.taran.arklib.domain.Message
import space.taran.arklib.utils.LogTags.RESOURCES_INDEX
import space.taran.arklib.utils.withContextAndLock
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi

class UpdatedResourcesId(
    val deleted: Set<ResourceId>,
    val added: Map<ResourceId, Path>
)

class UpdatedResources(
    val deleted: Set<ResourceId>,
    val added: Map<Resource, Path>
)

@OptIn(ExperimentalPathApi::class)
class PlainIndex internal constructor(
    private val root: Path,
    private val messageFlow: MutableSharedFlow<Message>,
    resources: Map<Path, Resource>
) : ResourceIndex {

    private val mutex = Mutex()
    private val mutUpdatedResourcesFlow = MutableSharedFlow<UpdatedResources>()

    val updatedResourcesFlow = mutUpdatedResourcesFlow.asSharedFlow()

    internal val resourceByPath: MutableMap<Path, Resource> =
        resources.toMutableMap()

    private val pathById: MutableMap<ResourceId, Path> =
        resources.map { (path, resource) ->
            resource.id to path
        }
            .toMap()
            .toMutableMap()

    override suspend fun allResources(
        prefix: Path?
    ): Set<Resource> = mutex.withLock {
        val filtered = if (prefix != null) {
            resourceByPath.filterKeys { it.startsWith(prefix) }
        } else {
            resourceByPath
        }.values

        Log.d(RESOURCES_INDEX, "${filtered.size} resources returned")
        return filtered.toSet()
    }

    fun contains(id: ResourceId) = pathById.containsKey(id)

    override suspend fun getPath(id: ResourceId): Path = mutex.withLock {
        tryGetPath(id)!!
    }

    override suspend fun getResource(id: ResourceId): Resource = mutex.withLock {
        tryGetMeta(id)!!
    }

    override suspend fun updateAll(): Unit =
        withContextAndLock(Dispatchers.IO, mutex) {
            val update = BindingIndex.update(root)
            handleUpdate(update)

            BindingIndex.store(root)
        }

    private suspend fun handleUpdate(update: UpdatedResourcesId) {
        update.deleted.forEach { id ->
            val path = pathById.remove(id)
            resourceByPath.remove(path)
        }

        val added = update.added
            //we can't present resources without low-level details
            .mapNotNull { (id, path) ->
                val resource: Resource = Resource.compute(id, path)
                    .onFailure { error ->
                        Log.e(
                            RESOURCES_INDEX,
                            "Couldn't compute resource by path $path: $error"
                        )

                        return@mapNotNull null
                    }.getOrThrow()


                resourceByPath[path] = resource
                pathById[id] = path

                resource to path
            }.toMap()

        mutUpdatedResourcesFlow.emit(UpdatedResources(update.deleted, added))
    }

    // should be only used in AggregatedResourcesIndex
    fun tryGetPath(id: ResourceId): Path? = pathById[id]

    // should be only used in AggregatedResourcesIndex
    fun tryGetMeta(id: ResourceId): Resource? {
        val path = tryGetPath(id)
        if (path != null) {
            return resourceByPath[path]
        }
        return null
    }
}
