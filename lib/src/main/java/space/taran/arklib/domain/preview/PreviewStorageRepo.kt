package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.domain.index.ResourceIndexRepo
import java.nio.file.Path

class PreviewStorageRepo(
    private val foldersRepo: FoldersRepo,
    private val indexRepo: ResourceIndexRepo,
    private val appScope: CoroutineScope
) {
    private val storageByRoot = mutableMapOf<Path, PlainPreviewStorage>()

    suspend fun provide(rootAndFav: RootAndFav): PreviewStorage {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        val shards = roots.map { root ->
            storageByRoot[root] ?: PlainPreviewStorage(
                root,
                indexRepo.providePlainIndex(root),
                appScope
            ).also {
                storageByRoot[root] = it
            }
        }

        return AggregatedPreviewStorage(shards, appScope)
    }

    suspend fun provide(root: Path): PreviewStorage =
        provide(RootAndFav(root.toString(), null))
}
