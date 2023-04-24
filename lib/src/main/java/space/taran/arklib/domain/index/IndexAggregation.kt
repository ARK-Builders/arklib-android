package space.taran.arklib.domain.index

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import space.taran.arklib.ResourceId
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

    override suspend fun allResources(): Set<Resource> =
        shards.flatMap { it.allResources() }
            .toSet()

    override suspend fun getResource(id: ResourceId): Resource? =
        shards.firstNotNullOfOrNull { it.getResource(id) }

    override suspend fun getPath(id: ResourceId): Path? =
        shards.firstNotNullOfOrNull { it.getPath(id) }

    override suspend fun updateAll() {
        shards.forEach { it.updateAll() }
    }
}