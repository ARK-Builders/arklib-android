package space.taran.arklib.domain.meta

import space.taran.arklib.domain.index.ResourceIndex
import java.nio.file.Path

class MetadataStorageRepo() {
    private val storageByRoot = mutableMapOf<Path, PlainMetadataStorage>()

    //todo: deduplicate (similar code in PreviewStorageRepo)
    fun provide(index: ResourceIndex): MetadataStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { root ->
                storageByRoot[root.path] ?: PlainMetadataStorage(
                    root.path
                ).also {
                    storageByRoot[root.path] = it
                }
            }

            AggregatedMetadataStorage(shards)
        } else {
            val root = roots.iterator().next()
            val storage = PlainMetadataStorage(root.path)

            storageByRoot[root.path] = storage
            storage
        }
    }
}
