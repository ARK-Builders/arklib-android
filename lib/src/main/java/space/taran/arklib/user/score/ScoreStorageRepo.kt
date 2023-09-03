package dev.arkbuilders.arklib.user.score

import kotlinx.coroutines.CoroutineScope
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.RootIndex
import java.nio.file.Path

class ScoreStorageRepo(private val scope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, RootScoreStorage>()

    suspend fun provide(index: ResourceIndex): ScoreStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) to it }
            AggregateScoreStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    suspend fun provide(root: RootIndex): RootScoreStorage {
        var storage = storageByRoot[root.path]

        if (storage == null) {
            storage = RootScoreStorage(scope, root.path)
            storage.init()
            storageByRoot[root.path] = storage
        } else {
            storage.refresh()
        }

        return storage
    }
}
