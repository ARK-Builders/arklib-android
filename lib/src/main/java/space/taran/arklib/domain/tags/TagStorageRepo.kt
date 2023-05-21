package space.taran.arklib.domain.tags

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class TagsStorageRepo(private val scope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, RootTagsStorage>()

    suspend fun provide(index: ResourceIndex): Any {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }
            AggregateTagStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    suspend fun provide(root: RootIndex): RootTagsStorage {
        var storage = storageByRoot[root.path]

        if (storage == null) {
            storage = RootTagsStorage(scope, root.path)
            storage.init()
            storageByRoot[root.path] = storage
        } else {
            storage.refresh()
        }

        return storage
    }
}