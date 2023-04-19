package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.meta.RootMetadataStorage
import java.nio.file.Path

class PreviewStorageRepo(
    private val appScope: CoroutineScope,
    private val metadataStorageRepo: MetadataStorageRepo) {

    private val storageByRoot = mutableMapOf<Path, RootPreviewStorage>()

    fun provide(index: ResourceIndex): PreviewStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map {
                val metadataStorage = metadataStorageRepo.provide(it)
                provide(it, metadataStorage)
            }

            AggregatedPreviewStorage(shards, appScope)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    fun provide(
        root: RootIndex,
        metadataStorage: RootMetadataStorage
    ): RootPreviewStorage =

        storageByRoot[root.path] ?: RootPreviewStorage(
            root.path, metadataStorage, appScope
        ).also {
            storageByRoot[root.path] = it
        }
}
