package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.meta.MetadataProcessorRepo
import space.taran.arklib.domain.meta.RootMetadataProcessor
import java.nio.file.Path

class PreviewProcessorRepo(
    private val scope: CoroutineScope,
    private val metadataProcessorRepo: MetadataProcessorRepo) {

    private val processorByRoot = mutableMapOf<Path, RootPreviewProcessor>()

    fun provide(index: ResourceIndex): PreviewProcessor {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map {
                val metadataStorage = metadataProcessorRepo.provide(it)
                provide(it, metadataStorage)
            }

            AggregatePreviewProcessor(shards)
        } else {
            val root = roots.iterator().next()
            val metadataStorage = metadataProcessorRepo.provide(root)
            provide(root, metadataStorage)
        }
    }

    fun provide(
        root: RootIndex,
        metadataProcessor: RootMetadataProcessor
    ): RootPreviewProcessor =
        processorByRoot[root.path] ?: RootPreviewProcessor(
            scope, root, metadataProcessor
        ).also {
            processorByRoot[root.path] = it
        }
}
