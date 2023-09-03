package dev.arkbuilders.arklib.user.score

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.RootIndex
import dev.arkbuilders.arklib.data.storage.AggregateStorage
import dev.arkbuilders.arklib.data.storage.Storage

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
