package dev.arkbuilders.arklib.user.score

import kotlinx.coroutines.CoroutineScope
import dev.arkbuilders.arklib.arkFolder
import dev.arkbuilders.arklib.arkScores
import dev.arkbuilders.arklib.data.storage.FileStorage
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
