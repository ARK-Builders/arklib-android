package space.taran.arklib.domain.score

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.storage.AggregateStorage
import space.taran.arklib.domain.storage.Storage

interface ScoreStorage: Storage<Score> {

    fun getScore(id: ResourceId): Score =
        getValue(id)

    fun setScore(id: ResourceId, score: Score) =
        setValue(id, score)

    fun resetScores(ids: List<ResourceId>) =
        ids.forEach { remove(it) }
}

class AggregateScoreStorage(
    shards: Collection<Pair<RootScoreStorage, ResourceIndex>>
): AggregateStorage<Score>(ScoreMonoid, shards), ScoreStorage