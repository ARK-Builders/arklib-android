package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class PreviewStorageRepo(private val appScope: CoroutineScope) {
    private val storageByRoot = mutableMapOf<Path, PlainPreviewStorage>()

    //todo: deduplicate (similar code in MetadataStorageRepo)
    fun provide(index: ResourceIndex): PreviewStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }

            AggregatedPreviewStorage(shards, appScope)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    fun provide(root: RootIndex): PlainPreviewStorage =
        storageByRoot[root.path] ?: PlainPreviewStorage(
            root, appScope
        ).also {
            storageByRoot[root.path] = it
        }
}
