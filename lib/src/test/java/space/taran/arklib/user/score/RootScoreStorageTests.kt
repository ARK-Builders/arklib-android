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

    private val mScope: CoroutineScope by lazy {
        GlobalScope
    }

    private val mPathMock: Path by lazy {
        mockk()
    }

    private lateinit var mRootScoreStorage: RootScoreStorage

    @Before
    fun setup() {
        coEvery { mPathMock.resolve(any<String>()) } returns mPathMock
        coEvery { mPathMock.resolve(any<Path>()) } returns mPathMock
        mRootScoreStorage = RootScoreStorage(mScope, mPathMock)
    }

    @Test
    fun verifyGetScoreIfNoData() {
        val resourceId = randomResourceId()
        assertEquals(mRootScoreStorage.getScore(resourceId), ScoreMonoid.neutral)
    }

    @Test
    fun verifySetScore() {
        val resourceId = randomResourceId()
        val score = randomScore()
        mRootScoreStorage.setScore(resourceId, score)
        assertEquals(score, mRootScoreStorage.getScore(resourceId))
    }

    @Test
    fun verifyRemove() {
        val resourceId = randomResourceId()
        val score = randomScore()
        mRootScoreStorage.setScore(resourceId, score)
        var result = mRootScoreStorage.getScore(resourceId)
        assertEquals(score, result)
        mRootScoreStorage.remove(resourceId)
        result = mRootScoreStorage.getScore(resourceId)
        assertEquals(result, ScoreMonoid.neutral)
    }
}