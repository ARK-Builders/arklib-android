package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.ResourceIndexRepo
import java.nio.file.Path

class PreviewStorageRepo(
    private val foldersRepo: FoldersRepo,
    private val indexRepo: ResourceIndexRepo,
    private val appScope: CoroutineScope
) {
    private val storageByRoot = mutableMapOf<Path, PlainPreviewStorage>()

    suspend fun provide(index: ResourceIndex): PreviewStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { root ->
                storageByRoot[root.path] ?: PlainPreviewStorage(
                    root.path, root.updates, appScope
                ).also {
                    storageByRoot[root.path] = it
                }
            }

            AggregatedPreviewStorage(shards, appScope)
        } else {
            val root = roots.iterator().next()
            val storage = PlainPreviewStorage(root.path, root.updates, appScope)

            storageByRoot[root.path] = storage
            storage
        }
    }
}
