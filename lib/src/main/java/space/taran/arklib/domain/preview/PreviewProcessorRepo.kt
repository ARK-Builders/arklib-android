package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.meta.MetadataProcessorRepo
import space.taran.arklib.domain.meta.RootMetadataProcessor
import java.nio.file.Path

class PreviewProcessorRepo(
    private val scope: CoroutineScope,
    private val metadataProcessorRepo: MetadataProcessorRepo
) {

    private val processorByRoot = mutableMapOf<Path, RootPreviewProcessor>()

    suspend fun provide(index: ResourceIndex): PreviewProcessor {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map {
                val metadataStorage = metadataProcessorRepo.provide(it)
                provide(it, metadataStorage) to it
            }

            AggregatePreviewProcessor.provide(scope, shards)
        } else {
            val root = roots.iterator().next()
            val metadataStorage = metadataProcessorRepo.provide(root)
            provide(root, metadataStorage)
        }
    }

    suspend fun provide(
        root: RootIndex,
        metadataProcessor: RootMetadataProcessor
    ): RootPreviewProcessor =
        processorByRoot[root.path] ?: RootPreviewProcessor.provide(
            scope, root, metadataProcessor
        ).also {
            processorByRoot[root.path] = it
        }
}
