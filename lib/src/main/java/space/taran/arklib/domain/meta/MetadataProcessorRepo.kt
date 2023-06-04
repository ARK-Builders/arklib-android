package space.taran.arklib.domain.meta

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class MetadataProcessorRepo(private val scope: CoroutineScope) {
    private val processorByRoot = mutableMapOf<Path, RootMetadataProcessor>()

    suspend fun provide(index: ResourceIndex): MetadataProcessor {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }

            AggregateMetadataProcessor.provide(scope, shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

     suspend fun provide(root: RootIndex): RootMetadataProcessor =
        processorByRoot[root.path] ?: RootMetadataProcessor.provide(
            scope, root
        ).also {
            processorByRoot[root.path] = it
        }
}
