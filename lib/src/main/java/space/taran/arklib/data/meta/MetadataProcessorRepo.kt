package dev.arkbuilders.arklib.data.meta

import kotlinx.coroutines.CoroutineScope
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.RootIndex
import java.nio.file.Path

class MetadataProcessorRepo(private val scope: CoroutineScope) {
    private val processorByRoot = mutableMapOf<Path, RootMetadataProcessor>()

    suspend fun provide(index: ResourceIndex): MetadataProcessor {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) to it }

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
