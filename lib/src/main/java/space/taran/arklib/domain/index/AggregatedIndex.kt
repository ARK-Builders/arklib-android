package space.taran.arklib.domain.index

import space.taran.arklib.ResourceId
import java.nio.file.Path

/**
 * [AggregatedIndex] is useful for "aggregated" navigation mode — a mode in
 * which we navigate through multiple indexed folders (roots). For a single-root
 * navigation [PlainIndex] can be used, and both [AggregatedIndex]
 * and [PlainIndex] should be transparently interchangeable, meaning that
 * both implementations support the same methods and they should not be used
 * otherwise as by [ResourceIndex] interface. Any component using [ResourceIndex]
 * should function in the same fashion independent on implementation of the index,
 * and only capability to look into multiple roots is gained by passing
 * [AggregatedIndex] into the component.
 *
 * @param shards A collection of individual [PlainIndex] to be aggregated.
 */
class AggregatedIndex(
    private val shards: Collection<PlainIndex>
) : ResourceIndex {

    override suspend fun allResources(prefix: Path?): Set<Resource> =
        shards.flatMap { it.allResources(prefix) }
            .toSet()

    override suspend fun getPath(id: ResourceId): Path =
        tryShards { it.tryGetPath(id) }

    override suspend fun getResource(id: ResourceId): Resource =
        tryShards { it.tryGetMeta(id) }

    private fun <R> tryShards(f: (shard: PlainIndex) -> R?): R {
        shards.iterator()
            .forEach { shard ->
                val result = f(shard)
                if (result != null) {
                    return@tryShards result
                }
            }
        throw AssertionError("At least one of shards must yield success")
    }

    override suspend fun updateAll() {
        shards.forEach { it.updateAll() }
    }
}
