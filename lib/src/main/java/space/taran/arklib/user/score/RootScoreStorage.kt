package space.taran.arklib.user.score

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.arkFolder
import space.taran.arklib.arkScores
import space.taran.arklib.domain.storage.FileStorage
import java.nio.file.Path

class RootScoreStorage(
    scope: CoroutineScope,
    val root: Path
) : FileStorage<Score>(
    "scores", scope, root.arkFolder().arkScores(), ScoreMonoid
), ScoreStorage {

    override fun valueToString(value: Score): String =
        value.toString()
    override fun valueFromString(raw: String): Score =
        raw.toInt()
}
