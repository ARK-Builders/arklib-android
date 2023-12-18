package dev.arkbuilders.arklib.data.index

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import dev.arkbuilders.arklib.ResourceId
import java.nio.file.Path

/**
 * [IndexAggregation] is useful for "aggregated" navigation mode — a mode in
 * which we navigate through multiple indexed folders (roots). For a single-root
 * navigation [RootIndex] can be used, and both [IndexAggregation]
 * and [RootIndex] should be transparently interchangeable, meaning that
 * both implementations support the same methods and they should not be used
 * otherwise as by [ResourceIndex] interface. Any component using [ResourceIndex]
 * should function in the same fashion independent on implementation of the index,
 * and only capability to look into multiple roots is gained by passing
 * [IndexAggregation] into the component.
 *
 * @param shards A collection of individual [RootIndex] to be aggregated.
 */
class IndexAggregation(
    private val shards: Collection<RootIndex>
) : ResourceIndex {

    override val roots: Set<RootIndex> = shards.toSet()

    override val updates: Flow<ResourceUpdates> = shards
        .map { it.updates }
        .asIterable()
        .merge()

    override fun allResources(): Map<ResourceId, Resource> =
        shards
            .map { it.allResources() }
            .fold(hashMapOf()) { acc, curr ->
                acc.putAll(curr)
                acc
            }

    override fun getResource(id: ResourceId): Resource? =
        shards.firstNotNullOfOrNull { it.getResource(id) }

    override fun allPaths(): Map<ResourceId, Path> =
        shards
            .map { it.allPaths() }
            .fold(hashMapOf()) { acc, curr ->
                acc.putAll(curr)
                acc
            }

    override fun getPath(id: ResourceId): Path? =
        shards.firstNotNullOfOrNull { it.getPath(id) }

    override suspend fun updateAll() {
        shards.forEach { it.updateAll() }
    }

    override suspend fun updateOne(
        resourcePath: Path,
        oldId: ResourceId
    ): ResourceUpdates {
        return shards.find { resourcePath.startsWith(it.path) }
            ?.updateOne(resourcePath, oldId)
            ?: error(
                "At least one shard must contain the passed path" +
                        "shards: ${shards.map { it.path }} path: $resourcePath"
            )
    }
}