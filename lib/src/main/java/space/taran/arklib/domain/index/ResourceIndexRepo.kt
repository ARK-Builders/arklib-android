package space.taran.arklib.domain.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import java.nio.file.Path

class ResourceIndexRepo(
    private val foldersRepo: FoldersRepo,
) {
    suspend fun provide(
        rootAndFav: RootAndFav
    ): ResourceIndex = withContext(Dispatchers.IO) {
        val roots = foldersRepo.resolveRoots(rootAndFav)
        val shards = roots.map { RootIndex(it) }

        return@withContext IndexAggregation(shards)
    }

    suspend fun provide(
        root: Path,
    ): ResourceIndex = provide(
        RootAndFav(root.toString(), favString = null)
    )
}
