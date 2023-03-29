package space.taran.arklib.domain.index

import android.util.Log
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
import space.taran.arklib.utils.LogTags.RESOURCES_INDEX
import java.nio.file.Path

class ResourceIndexRepo(
    private val foldersRepo: FoldersRepo,
    private val metadataStorageRepo: MetadataStorageRepo,
    private val messageFlow: MutableSharedFlow<Message>,
) {
    private val provideMutex = Mutex()

    suspend fun provide(
        rootAndFav: RootAndFav
    ): ResourceIndex = withContext(Dispatchers.IO) {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        val indexShards = roots.map { root ->
            providePlainIndex(root)
        }

        return@withContext AggregatedIndex(indexShards)
    }

    suspend fun provide(
        root: Path,
    ): ResourceIndex = provide(
        RootAndFav(root.toString(), favString = null)
    )

    internal suspend fun providePlainIndex(
        root: Path
    ): PlainIndex = provideMutex.withLock {
        if (!BindingIndex.load(root)) {
            Log.e(
                RESOURCES_INDEX,
                "Couldn't provide index from $root"
            )
            throw NotImplementedError()
        }

        //id2path should be used in order to filter-out duplicates
        //path2id could contain several paths for the same id
        val resources: Map<Path, Resource> = BindingIndex.id2path(root)
            //we can't present resources without low-level details
            .mapNotNull { entry ->
                val id = entry.key
                val path = entry.value

                val resource: Resource = Resource.compute(id, path)
                    .onFailure { error ->
                        Log.e(
                            RESOURCES_INDEX,
                            "Couldn't compute resource by path $path: $error"
                        )

                        return@mapNotNull null
                    }.getOrThrow()

                path to resource
            }.toMap()

        return PlainIndex(
            root,
            messageFlow,
            resources
        )
    }
}
