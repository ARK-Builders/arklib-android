package space.taran.arklib.domain.score

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkScores
import space.taran.arklib.domain.storage.FileStorage
import java.nio.file.Path

class RootScoreStorage(
    scope: CoroutineScope,
    val root: Path) : FileStorage<Score>(
        scope, root.arkFolder().arkScores(), ScoreMonoid, "scores") {

    override fun valueToString(value: Score): String =
        value.toString()
    override fun valueFromString(raw: String): Score =
        raw.toInt()

    fun getScore(id: ResourceId): Score =
        getValue(id) as Score

    fun setScore(id: ResourceId, score: Score) =
        setValue(id, score)

    fun resetScores(ids: List<ResourceId>) =
        ids.forEach { remove(it) }
}
