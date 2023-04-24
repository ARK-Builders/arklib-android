package space.taran.arklib.domain.score

import space.taran.arklib.ResourceId
import java.lang.IllegalStateException
import java.nio.file.Path

class AggregatedScoreStorage(
    private val shards: Collection<RootScoreStorage>
) {

    fun locate(path: Path, id: ResourceId): Score = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            if (it == null) {
                throw IllegalStateException("Shard must be in the aggregation")
            }

            it.getScore(id)
        }
}
