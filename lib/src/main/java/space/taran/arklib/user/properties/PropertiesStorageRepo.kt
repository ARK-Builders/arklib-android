package dev.arkbuilders.arklib.user.properties

import kotlinx.coroutines.CoroutineScope
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.RootIndex
import java.nio.file.Path

class PropertiesStorageRepo(private val scope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, RootPropertiesStorage>()

    suspend fun provide(index: ResourceIndex): PropertiesStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) to it }
            AggregatePropertiesStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    suspend fun provide(root: RootIndex): RootPropertiesStorage {
        var storage = storageByRoot[root.path]

        if (storage == null) {
            storage = RootPropertiesStorage(scope, root.path)
            storage.init()
            storageByRoot[root.path] = storage
        } else {
            storage.refresh()
        }

        return storage
    }
}
