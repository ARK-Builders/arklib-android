package space.taran.arklib.user

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.user.score.Score
import dev.arkbuilders.arklib.user.score.ScoreStorage
import org.hamcrest.core.IsInstanceOf.any
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScoreStorageTests {

    private lateinit var mTestObj: ScoreStorage

    private var mSetValueCalled: Boolean = false
    private val mValue by lazy {
        (Math.random() * 100).toInt()
    }
    private var mRemoveCalled: Boolean = false
    private val mIdList: MutableList<ResourceId> = mutableListOf()
    private lateinit var mPair: Pair<ResourceId, Score>

    @Before
    fun setup() {
        mTestObj = object : ScoreStorage {
            override fun getValue(id: ResourceId): Score {
                return mValue
            }

            override fun setValue(id: ResourceId, value: Score) {
                mSetValueCalled = true
                mPair = Pair(id, value)
            }

            override fun remove(id: ResourceId) {
                mRemoveCalled = true
                mIdList.add(id)
            }

            override suspend fun persist() {
            }
        }
    }

    @Test
    fun verifyScoreStorageGetValue() {
        assertEquals(mTestObj.getScore(ResourceId(0, 0)), mValue)
    }

    @Test
    fun verifyScoreStorageSetValue() {
        mSetValueCalled = false
        val resourceId = ResourceId(0, 9)
        mTestObj.setScore(resourceId, mValue)
        assertTrue(mSetValueCalled)
        assertEquals(resourceId, mPair.first)
        assertEquals(mValue, mPair.second)
    }

    @Test
    fun verifyRemove() {
        mRemoveCalled = false
        mIdList.clear()
        val listSize = (Math.random() * 10).toInt() + 1
        val list = mutableListOf<ResourceId>()
        for (index in 0 until listSize) {
            list.add(randomResourceId())
        }
        mTestObj.resetScores(list)
        assertTrue(mRemoveCalled)
        assertEquals(mIdList.size, list.size)
        assertEquals(mIdList, list)
    }

    private fun randomResourceId(): ResourceId =
        ResourceId((Math.random() * 100).toLong(), (Math.random() * 100).toLong())
}