package space.taran.arklib.user.score

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.user.score.RootScoreStorage
import dev.arkbuilders.arklib.user.score.Score
import dev.arkbuilders.arklib.user.score.ScoreMonoid
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.GlobalScope
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import space.taran.arklib.randomPositiveInt
import space.taran.arklib.randomResourceId
import space.taran.arklib.randomScore
import java.nio.file.Path

class RootScoreStorageTests {

    private val pathMock: Path by lazy {
        mockk()
    }

    @Before
    fun setup() {
        coEvery { pathMock.resolve(any<String>()) } returns pathMock
        coEvery { pathMock.resolve(any<Path>()) } returns pathMock
    }

    @Test
    fun getScoreReturnsScoreMonidIfNotData() {
        val rootScoreStorage = RootScoreStorage(GlobalScope, pathMock)
        val resourceId = randomResourceId()
        assertEquals(rootScoreStorage.getScore(resourceId), ScoreMonoid.neutral)
    }

    @Test
    fun setScoreResultsInCorrectValueBeingReturned() {
        val rootScoreStorage = RootScoreStorage(GlobalScope, pathMock)
        val resourceId = randomResourceId()
        val score = randomScore()
        rootScoreStorage.setScore(resourceId, score)
        assertEquals(score, rootScoreStorage.getScore(resourceId))
    }

    @Test
    fun callToRemoveClearsCorrespondingData() {
        val rootScoreStorage = RootScoreStorage(GlobalScope, pathMock)
        val resourceId = randomResourceId()
        val score = randomScore()
        rootScoreStorage.setScore(resourceId, score)
        var result = rootScoreStorage.getScore(resourceId)
        assertEquals(score, result)
        rootScoreStorage.remove(resourceId)
        result = rootScoreStorage.getScore(resourceId)
        assertEquals(result, ScoreMonoid.neutral)
    }

    @Test
    fun multipleFunctionCallsInSerialOrder() {
        val rootScoreStorage = RootScoreStorage(GlobalScope, pathMock)

        val totalSetRandomValue = randomPositiveInt(10) + 20
        val map = mutableMapOf<ResourceId, Score>()

        for (index in 1..totalSetRandomValue) {
            val resourceId = randomResourceId()
            val score = randomScore()
            map[resourceId] = score
            rootScoreStorage.setScore(resourceId, score)
        }

        for (resourceId in map.keys) {
            val collectedValue = rootScoreStorage.getValue(resourceId)
            val expectedValue = map[resourceId]
            assertEquals(expectedValue, collectedValue)
        }

        for (resourceId in map.keys) {
            rootScoreStorage.remove(resourceId)
            val collectedValue = rootScoreStorage.getValue(resourceId)
            assertEquals(collectedValue, ScoreMonoid.neutral)
        }
    }

    @Test
    fun multipleFunctionCallsInRandomisedOrder() {
        val rootScoreStorage = RootScoreStorage(GlobalScope, pathMock)

        val maxNumberOfCalls = randomPositiveInt(20) + 20

        val randomIntervals = mutableSetOf<Int>()
        var index = 0
        while (index < 5) {
            val value = randomPositiveInt(maxNumberOfCalls)
            if (randomIntervals.contains(value).not()) {
                index++
                randomIntervals.add(value)
            }
        }

        val mapOfResIdToScore = mutableMapOf<ResourceId, Score>()

        for (i in 1..maxNumberOfCalls) {
            val randomResourceId = randomResourceId()
            val randomScore = randomScore()
            mapOfResIdToScore[randomResourceId] = randomScore
            rootScoreStorage.setScore(randomResourceId, randomScore)

            if (randomIntervals.contains(i)) {
                for (resId in mapOfResIdToScore.keys) {
                    val expectedValue = mapOfResIdToScore[resId]
                    val collectedValue = rootScoreStorage.getValue(resId)
                    assertEquals(expectedValue, collectedValue)
                }
                mapOfResIdToScore.remove(randomResourceId)
                rootScoreStorage.remove(randomResourceId)
                assertEquals(rootScoreStorage.getScore(randomResourceId), ScoreMonoid.neutral)
            }
        }
    }
}