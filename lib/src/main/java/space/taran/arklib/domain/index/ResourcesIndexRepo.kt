package space.taran.arklib.domain.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.dao.ResourceDao
import space.taran.arklib.domain.index.PlainResourcesIndex.Companion.loadResources
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arklib.utils.Constants
import space.taran.arklib.utils.LogTags.RESOURCES_INDEX
import java.nio.file.Path
import javax.inject.Named

class ResourcesIndexRepo(
    private val dao: ResourceDao,
    private val foldersRepo: FoldersRepo,
    private val previewStorageRepo: PreviewStorageRepo,
    private val metadataStorageRepo: MetadataStorageRepo,
    @Named(Constants.DI.MESSAGE_FLOW_NAME)
    private val messageFlow: MutableSharedFlow<Message>,
) {
    private val provideMutex = Mutex()
    private val indexByRoot = mutableMapOf<Path, PlainResourcesIndex>()

    private suspend fun loadFromDatabase(
        root: Path
    ): PlainResourcesIndex = withContext(Dispatchers.IO) {
        Log.d(
            RESOURCES_INDEX,
            "loading index for $root from the database"
        )

        val resources = dao.query(root.toString())

        Log.d(
            RESOURCES_INDEX,
            "${resources.size} resources retrieved from DB"
        )

        return@withContext PlainResourcesIndex(
            root,
            dao,
            previewStorageRepo.provide(root),
            metadataStorageRepo.provide(root),
            messageFlow,
            loadResources(resources)
        )
    }

    suspend fun provide(
        rootAndFav: RootAndFav
    ): ResourcesIndex = withContext(Dispatchers.IO) {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        provideMutex.withLock {
            val indexShards = roots.map { root ->
                indexByRoot[root] ?: let {
                    val index = loadFromDatabase(root)
                    indexByRoot[root] = index
                    index
                }
            }

            return@withContext AggregatedResourcesIndex(indexShards)
        }
    }

    suspend fun provide(
        root: Path,
    ): ResourcesIndex = provide(
        RootAndFav(root.toString(), favString = null)
    )

    suspend fun isIndexed(rootAndFav: RootAndFav): Boolean {
        val roots = foldersRepo.resolveRoots(rootAndFav)
        roots.forEach { root ->
            if (!indexByRoot.contains(root))
                return false
        }
        return true
    }
}
