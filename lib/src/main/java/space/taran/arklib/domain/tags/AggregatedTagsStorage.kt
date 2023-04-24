package space.taran.arklib.domain.tags

import space.taran.arklib.ResourceId
import java.lang.IllegalStateException
import java.nio.file.Path

class AggregatedTagsStorage(
    private val shards: Collection<RootTagsStorage>
) {

    fun locate(path: Path, id: ResourceId): Tags = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            if (it == null) {
                throw IllegalStateException("Shard must be in the aggregation")
            }

            it.getTags(id)
        }
}
