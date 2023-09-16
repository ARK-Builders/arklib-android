package dev.arkbuilders.arklib.data.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.binding.BindingIndex
import dev.arkbuilders.arklib.binding.RawUpdates
import dev.arkbuilders.arklib.utils.withContextAndLock
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * [RootIndex] is a type of index backed by storage file.
 * It is made to store ids of all resources in a "root folder",
 * which is:
 * 1) a unit of file synchronization,
 *    i.e. we either sync all of its content or none;
 * 2) a mounting point in resource space,
 *    i.e. total resource space is a union of resources
 *    from all root folders.
 * @param path A path to the root folder.
 *
 * See also [IndexAggregation] and [IndexProjection].
 */
class RootIndex private constructor(val path: Path) : ResourceIndex {

    companion object {
        suspend fun provide(path: Path): RootIndex {
            val result = RootIndex(path)
            result.init()

            return result
        }
    }

    private val _updates = MutableSharedFlow<ResourceUpdates>()

    private val mutex = Mutex()

    private val resourceAndPathById:
            ConcurrentHashMap<ResourceId, Pair<Resource, Path>> = ConcurrentHashMap()

    private fun wrap(update: RawUpdates): ResourceUpdates {
        Log.d(LOG_PREFIX, "wrapping raw updates from arklib")

        val deleted = update.deleted.associateWith { id ->
            val (resource, path) = resourceAndPathById[id]!!
            LostResource(path, resource)
        }

        val added = update.added
            //we can't present empty resources
            .mapNotNull { (id, path) ->
                val resource: Resource = Resource.compute(id, path)
                    .onFailure { error ->
                        Log.e(
                            LOG_PREFIX,
                            "Couldn't compute resource by path $path: $error"
                        )

                        return@mapNotNull null
                    }.getOrThrow()

                id to NewResource(path, resource)
            }.toMap()

        return ResourceUpdates(deleted, added)
    }

    suspend fun init() {
        Log.i(LOG_PREFIX, "Initializing new index for root $path")
        withContextAndLock(Dispatchers.IO, mutex) {
            if (!BindingIndex.load(path)) {
                Log.e(
                    LOG_PREFIX,
                    "Couldn't provide index from $path"
                )
                throw UnknownError()
            }

            // when index instance is already created,
            // right now, we do the updating twice
            // if the index is created

            // when the index is initialized,
            // we should not have subscribers yet
            // this update is not pushed into the flow
            // it is needed only to catch up
            BindingIndex.updateAll(path)
            BindingIndex.store(path)

            // id2path should be used in order to filter-out duplicates
            // path2id could contain several paths for the same id
            BindingIndex
                .id2path(path)
                .forEach { (id, path) ->
                    Resource.compute(id, path)
                        .onFailure { error ->
                            Log.e(LOG_PREFIX, error.toString())
                        }
                        .onSuccess { resource ->
                            resourceAndPathById[id] = resource to path
                        }
                }
        }

        check()
    }

    override val roots: Set<RootIndex> = setOf(this)

    override val updates = _updates.asSharedFlow()

    override suspend fun updateAll(): Unit =
        withContextAndLock(Dispatchers.IO, mutex) {
            Log.i(LOG_PREFIX, "Updating the index of root $path")

            val raw: RawUpdates = BindingIndex.updateAll(path)
            BindingIndex.store(path)

            val updates: ResourceUpdates = wrap(raw)

            updates.deleted.forEach { (id, _) ->
                resourceAndPathById.remove(id)
            }

            updates.added.forEach { (id, added) ->
                resourceAndPathById[id] = added.resource to added.path
            }

            _updates.emit(updates)
            check()
        }

    override fun allResources(): Map<ResourceId, Resource> =
        resourceAndPathById.mapValues { it.value.first }

    override fun getResource(id: ResourceId): Resource? =
        resourceAndPathById[id]?.first

    override fun allPaths(): Map<ResourceId, Path> =
        resourceAndPathById.mapValues { it.value.second }

    override fun getPath(id: ResourceId): Path? =
        resourceAndPathById[id]?.second

    // used by storages to initialize state
    internal fun asAdded(): Set<NewResource> =
        resourceAndPathById.map { (id, pair) ->
            val (resource, path) = pair
            NewResource(path, resource)
        }.toSet()


    private fun check() {
        val resourceById = resourceAndPathById.mapValues { it.value.first }
        val pathById = resourceAndPathById.mapValues { it.value.second }
        val idsN1 = pathById.keys.size
        val idsN2 = resourceById.keys.size
        val pathsN = pathById.values.size
        val resourcesN = resourceById.values.size

        Log.i(LOG_PREFIX, "There are $pathsN paths in the index")
        Log.i(LOG_PREFIX, "There are $resourcesN resources in the index")
        Log.d(LOG_PREFIX, "there are $idsN1 ids in paths map")
        Log.d(LOG_PREFIX, "there are $idsN2 ids in resources map")

        val duplicatesN = pathsN - resourcesN
        if (duplicatesN > 0) {
            Log.w(LOG_PREFIX, "There are $duplicatesN duplicates in the root")
        }

        if (idsN1 != idsN2) {
            throw IllegalStateException("Different amount of ids in the index maps")
        }
    }
}
