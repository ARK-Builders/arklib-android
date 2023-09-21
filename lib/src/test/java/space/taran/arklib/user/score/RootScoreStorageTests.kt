package space.taran.arklib.user.score

import dev.arkbuilders.arklib.user.score.RootScoreStorage
import dev.arkbuilders.arklib.user.score.ScoreMonoid
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import space.taran.arklib.randomResourceId
import space.taran.arklib.randomScore
import java.nio.file.Path

class RootScoreStorageTests {

    private val scope: CoroutineScope by lazy {
        GlobalScope
    }

    private val pathMock: Path by lazy {
        mockk()
    }

    private lateinit var rootScoreStorage: RootScoreStorage

    @Before
    fun setup() {
        coEvery { pathMock.resolve(any<String>()) } returns pathMock
        coEvery { pathMock.resolve(any<Path>()) } returns pathMock
        rootScoreStorage = RootScoreStorage(scope, pathMock)
    }

    @Test
    fun verifyGetScoreIfNoData() {
        val resourceId = randomResourceId()
        assertEquals(rootScoreStorage.getScore(resourceId), ScoreMonoid.neutral)
    }

    @Test
    fun verifySetScore() {
        val resourceId = randomResourceId()
        val score = randomScore()
        rootScoreStorage.setScore(resourceId, score)
        assertEquals(score, rootScoreStorage.getScore(resourceId))
    }

    @Test
    fun verifyRemove() {
        val resourceId = randomResourceId()
        val score = randomScore()
        rootScoreStorage.setScore(resourceId, score)
        var result = rootScoreStorage.getScore(resourceId)
        assertEquals(score, result)
        rootScoreStorage.remove(resourceId)
        result = rootScoreStorage.getScore(resourceId)
        assertEquals(result, ScoreMonoid.neutral)
    }
}