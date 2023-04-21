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
        folders: RootAndFav
    ): ResourceIndex = withContext(Dispatchers.IO) {
        val roots = foldersRepo.resolveRoots(folders)

        if (roots.size > 1) {
            val shards = roots.map { RootIndex.provide(it) }
            return@withContext IndexAggregation(shards)
        } else {
            val root = roots.iterator().next()
            val index = RootIndex.provide(root)

            if (folders.fav != null) {
                val rootPath = folders.root!!
                val favoritePath = rootPath.resolve(folders.fav!!)

                return@withContext IndexProjection(index) { _, path ->
                    path.startsWith(favoritePath)
                }
            } else {
                return@withContext index
            }
        }
    }

    suspend fun provide(
        root: Path,
    ): ResourceIndex = provide(
        RootAndFav(root.toString(), favString = null)
    )
}
