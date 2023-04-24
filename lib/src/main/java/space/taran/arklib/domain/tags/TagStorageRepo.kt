package space.taran.arklib.domain.tags

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class TagsStorageRepo(private val scope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, RootTagsStorage>()

    fun provide(index: ResourceIndex): Any {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }
            AggregatedTagsStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    fun provide(root: RootIndex): RootTagsStorage =
        storageByRoot[root.path] ?: RootTagsStorage(
            scope, root.path,
        ).also {
            storageByRoot[root.path] = it
        }
}