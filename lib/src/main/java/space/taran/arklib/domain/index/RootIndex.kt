package space.taran.arklib.domain.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.taran.arklib.ResourceId
import space.taran.arklib.binding.BindingIndex
import space.taran.arklib.binding.RawUpdates
import space.taran.arklib.utils.LogTags
import space.taran.arklib.utils.LogTags.RESOURCES_INDEX
import space.taran.arklib.utils.withContextAndLock
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi

class ResourceUpdates (
    val deleted: Map<ResourceId, LostResource>,
    val added: Map<ResourceId, NewResource>
)

data class LostResource(val path: Path, val resource: Resource)

data class NewResource(val path: Path, val resource: Resource)

/**
 * [RootIndex] is a type of index backed by storage file.
 * It is made to store ids of all resources in a "root folder",
 * which is:
 * 1) a unit of file synchronization,
 *    i.e. we either sync all of its content or none;
 * 2) a mounting point in resource space,
 *    i.e. total resource space is a union of resources
 *    from all root folders.
 * @param root A path to the root folder.
 *
 * See also [IndexAggregation] and [IndexProjection].
 */
@OptIn(ExperimentalPathApi::class)
class RootIndex(val root: Path): ResourceIndex {

    private val mutex = Mutex()
    private val mutUpdates = MutableSharedFlow<ResourceUpdates>()

    private val resourceById: MutableMap<ResourceId, Resource> = mutableMapOf()
    private val pathById: MutableMap<ResourceId, Path> = mutableMapOf()

    private fun wrap(update: RawUpdates): ResourceUpdates {
        val deleted = update.deleted.associateWith { id ->
            val path = pathById[id]!!
            val resource = resourceById[id]!!
            LostResource(path, resource)
        }

        val added = update.added
            //we can't present empty resources
            .mapNotNull { (id, path) ->
                val resource: Resource = Resource.compute(id, path)
                    .onFailure { error ->
                        Log.e(
                            LogTags.RESOURCES_INDEX,
                            "Couldn't compute resource by path $path: $error"
                        )

                        return@mapNotNull null
                    }.getOrThrow()

                id to NewResource(path, resource)
            }.toMap()

        return ResourceUpdates(deleted, added)
    }

    init {
        if (!BindingIndex.load(root)) {
            Log.e(
                RESOURCES_INDEX,
                "Couldn't provide index from $root"
            )
            throw NotImplementedError()
        }

        //id2path should be used in order to filter-out duplicates
        //path2id could contain several paths for the same id
        BindingIndex.id2path(root)
            .forEach { (id, path) ->
                Resource.compute(id, path)
                    .onFailure { error ->
                        Log.e(
                            RESOURCES_INDEX,
                            "Couldn't compute resource by path $path: $error"
                        )
                    }
                    .onSuccess { resource ->
                        pathById[id] = path
                        resourceById[id] = resource
                    }
            }
    }

    override val updates = mutUpdates.asSharedFlow()

    override suspend fun updateAll(): Unit =
        withContextAndLock(Dispatchers.IO, mutex) {
            val raw: RawUpdates = BindingIndex.update(root)
            BindingIndex.store(root)

            val updates: ResourceUpdates = wrap(raw)

            updates.deleted.forEach { (id, _) ->
                resourceById.remove(id)
                pathById.remove(id)
            }

            updates.added.forEach { (id, added) ->
                resourceById[id] = added.resource
                pathById[id] = added.path
            }

            mutUpdates.emit(updates)
        }

    override suspend fun allResources(): Set<Resource> = mutex.withLock {
        return resourceById.values.toSet()
    }

    override suspend fun getResource(id: ResourceId): Resource? = mutex.withLock {
        return resourceById[id]
    }

    override suspend fun getPath(id: ResourceId): Path? = mutex.withLock {
        return pathById[id]
    }
}
