package space.taran.arklib.domain.meta

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class MetadataStorageRepo(private val appScope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, PlainMetadataStorage>()

    //todo: deduplicate (similar code in PreviewStorageRepo)
    fun provide(index: ResourceIndex): MetadataStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }

            AggregatedMetadataStorage(shards, appScope)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    fun provide(root: RootIndex): PlainMetadataStorage =
        storageByRoot[root.path] ?: PlainMetadataStorage(
            root, appScope
        ).also {
            storageByRoot[root.path] = it
        }
}
