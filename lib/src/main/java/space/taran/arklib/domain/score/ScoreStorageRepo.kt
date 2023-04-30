package space.taran.arklib.domain.score

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class ScoreStorageRepo(private val scope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, RootScoreStorage>()

    fun provide(index: ResourceIndex): Any {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }
            AggregateScoreStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    fun provide(root: RootIndex): RootScoreStorage =
        storageByRoot[root.path] ?: RootScoreStorage(
            scope, root.path,
        ).also {
            storageByRoot[root.path] = it
        }
}
