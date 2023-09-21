package space.taran.arklib.user.score

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.user.score.Score
import dev.arkbuilders.arklib.user.score.ScoreStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScoreStorageTests {

    private lateinit var testObj: ScoreStorage

    private var setValueCalled: Boolean = false
    private val randomValue by lazy {
        (Math.random() * 100).toInt()
    }
    private var isRemoveCalled: Boolean = false
    private val idList: MutableList<ResourceId> = mutableListOf()
    private lateinit var pair: Pair<ResourceId, Score>

    @Before
    fun setup() {
        testObj = object : ScoreStorage {
            override fun getValue(id: ResourceId): Score {
                return randomValue
            }

            override fun setValue(id: ResourceId, value: Score) {
                setValueCalled = true
                pair = Pair(id, value)
            }

            override fun remove(id: ResourceId) {
                isRemoveCalled = true
                idList.add(id)
            }

            override suspend fun persist() {
            }
        }
    }

    @Test
    fun verifyScoreStorageGetValue() {
        assertEquals(testObj.getScore(ResourceId(0, 0)), randomValue)
    }

    @Test
    fun verifyScoreStorageSetValue() {
        setValueCalled = false
        val resourceId = ResourceId(0, 9)
        testObj.setScore(resourceId, randomValue)
        assertTrue(setValueCalled)
        assertEquals(resourceId, pair.first)
        assertEquals(randomValue, pair.second)
    }

    @Test
    fun verifyRemove() {
        isRemoveCalled = false
        idList.clear()
        val listSize = (Math.random() * 10).toInt() + 1
        val list = mutableListOf<ResourceId>()
        for (index in 0 until listSize) {
            list.add(randomResourceId())
        }
        testObj.resetScores(list)
        assertTrue(isRemoveCalled)
        assertEquals(idList.size, list.size)
        assertEquals(idList, list)
    }

    private fun randomResourceId(): ResourceId =
        ResourceId((Math.random() * 100).toLong(), (Math.random() * 100).toLong())
}