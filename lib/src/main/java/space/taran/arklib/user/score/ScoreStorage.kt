package space.taran.arklib.user.score

import space.taran.arklib.ResourceId
import space.taran.arklib.data.index.ResourceIndex
import space.taran.arklib.data.index.RootIndex
import space.taran.arklib.data.storage.AggregateStorage
import space.taran.arklib.data.storage.Storage

interface ScoreStorage: Storage<Score> {

    fun getScore(id: ResourceId): Score =
        getValue(id)

    fun setScore(id: ResourceId, score: Score) =
        setValue(id, score)

    fun resetScores(ids: List<ResourceId>) =
        ids.forEach { remove(it) }
}

class AggregateScoreStorage(
    shards: Collection<Pair<RootScoreStorage, RootIndex>>
): AggregateStorage<Score>(ScoreMonoid, shards), ScoreStorage
