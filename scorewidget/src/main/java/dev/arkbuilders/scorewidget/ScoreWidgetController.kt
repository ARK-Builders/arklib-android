package dev.arkbuilders.scorewidget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.score.Score
import space.taran.arklib.domain.score.ScoreStorage

data class ScoreWidgetState(
    val score: Score,
    val visible: Boolean,
)

class ScoreWidgetController(
    val scope: CoroutineScope,
    val getCurrentId: () -> ResourceId,
    val onScoreChanged: (ResourceId) -> Unit
): ContainerHost<ScoreWidgetState, Unit> {

    private lateinit var scoreStorage: ScoreStorage

    override val container: Container<ScoreWidgetState, Unit> =
        scope.container(ScoreWidgetState(score = 0, visible = false))

    fun init(scoreStorage: ScoreStorage) {
        this.scoreStorage = scoreStorage
    }

    fun setVisible(visible: Boolean) = intent {
        reduce {
            state.copy(visible = visible)
        }
    }

    fun displayScore() = intent {
        reduce {
            state.copy(score = scoreStorage.getScore(getCurrentId()))
        }
    }

    fun onIncrease() = changeScore(1)

    fun onDecrease() = changeScore(-1)

    private fun changeScore(inc: Score) = scope.launch {
        val id = getCurrentId()
        val score = scoreStorage.getScore(id) + inc

        scoreStorage.setScore(id, score)
        withContext(Dispatchers.IO) {
            scoreStorage.persist()
        }
        intent {
            reduce {
                state.copy(score = score)
            }
        }
        onScoreChanged(id)
    }
}