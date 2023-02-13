package space.taran.arklib.domain.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.binding.BindingIndex
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arklib.utils.Constants
import java.nio.file.Path
import javax.inject.Named

class ResourcesIndexRepo(
    private val foldersRepo: FoldersRepo,
    private val previewStorageRepo: PreviewStorageRepo,
    private val metadataStorageRepo: MetadataStorageRepo,
    @Named(Constants.DI.MESSAGE_FLOW_NAME)
    private val messageFlow: MutableSharedFlow<Message>,
) {
    private val provideMutex = Mutex()
    private val indexByRoot = mutableMapOf<Path, PlainResourcesIndex>()

    private suspend fun create(
        root: Path
    ): PlainResourcesIndex = withContext(Dispatchers.IO) {
        val loaded = BindingIndex.load(root)
        val metadataStorage = metadataStorageRepo.provide(root)

        val resources = if (loaded) {
            BindingIndex.path2id(root).mapNotNull { entry ->
                val path = entry.key
                val (id, time) = entry.value

                ResourceMeta.fromPath(id, path, metadataStorage)
                    .onSuccess { return@mapNotNull path to it }

                return@mapNotNull null
            }.toMap()
        } else emptyMap()

        return@withContext PlainResourcesIndex(
            root,
            previewStorageRepo.provide(root),
            metadataStorage,
            messageFlow,
            loaded,
            resources
        )
    }

    suspend fun provide(
        rootAndFav: RootAndFav
    ): ResourcesIndex = withContext(Dispatchers.IO) {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        provideMutex.withLock {
            val indexShards = roots.map { root ->
                indexByRoot[root] ?: let {
                    val index = create(root)
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
