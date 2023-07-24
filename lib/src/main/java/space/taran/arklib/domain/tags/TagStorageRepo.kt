package space.taran.arklib.domain.tags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.stats.StatsEvent
import java.nio.file.Path

class TagsStorageRepo(
    private val scope: CoroutineScope,
    private val statsFlow: MutableSharedFlow<StatsEvent>,
) {
    private val storageByRoot = mutableMapOf<Path, RootTagsStorage>()

    suspend fun provide(index: ResourceIndex): TagStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) to it }
            AggregateTagStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    suspend fun provide(root: RootIndex): RootTagsStorage {
        var storage = storageByRoot[root.path]

        if (storage == null) {
            storage = RootTagsStorage(scope, root.path, statsFlow)
            storage.init()
            storageByRoot[root.path] = storage
        } else {
            storage.refresh()
        }

        return storage
    }
}