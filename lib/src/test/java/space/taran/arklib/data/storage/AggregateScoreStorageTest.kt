package space.taran.arklib.data.storage

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.RootIndex
import dev.arkbuilders.arklib.user.score.AggregateScoreStorage
import dev.arkbuilders.arklib.user.score.RootScoreStorage
import dev.arkbuilders.arklib.user.score.Score
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class AggregateScoreStorageTest {

    private val storedScores = HashMap<ResourceId, Score>()
    private val resourceIds: Set<ResourceId> = setOf(
        ResourceId(1024, 123456789),
        ResourceId(2048, 987654321),
        ResourceId(4096, 246810121),
        ResourceId(8192, 369121518),
    )

    private lateinit var shards: List<Pair<RootScoreStorage, RootIndex>>
    private lateinit var aggregateScoreStorage: AggregateScoreStorage

    @Before
    fun beforeEach() {
        storedScores.clear()
        shards = createShards(3)
        aggregateScoreStorage = AggregateScoreStorage(shards)
    }

    private fun createShards(amount: Int): List<Pair<RootScoreStorage, RootIndex>> {
        val shards = ArrayList<Pair<RootScoreStorage, RootIndex>>(amount)
        for (i in 1..amount) {
            val rootIndex = createRootIndex()
            val rootScoreStorage = createRootScoreStorage()
            shards.add(rootScoreStorage to rootIndex)
        }
        return shards
    }

    private fun createRootIndex(): RootIndex {
        val rootIndex = mockk<RootIndex>()
        every { rootIndex.allIds() } returns resourceIds
        return rootIndex;
    }

    private fun createRootScoreStorage(): RootScoreStorage {
        val rootScoreStorage = mockk<RootScoreStorage>()
        val scoreSlot = CapturingSlot<Score>()
        val resourceIdSlot = CapturingSlot<ResourceId>()
        every { rootScoreStorage.setValue(capture(resourceIdSlot), capture(scoreSlot)) } answers {
            val score = scoreSlot.captured
            val resourceId = resourceIdSlot.captured
            storedScores[resourceId] = score
        }
        every { rootScoreStorage.valueById[capture(resourceIdSlot)] } answers {
            val resourceId = resourceIdSlot.captured
            storedScores[resourceId]
        }
        every { rootScoreStorage.remove(capture(resourceIdSlot)) } answers {
            val resourceId = resourceIdSlot.captured
            storedScores.remove(resourceId)
        }
        return rootScoreStorage;
    }

    @After
    fun afterEach() {
        unmockkAll()
    }

    private fun getRandomStoragePairs(amount: Int): Map<ResourceId, Score> {
        if (amount > this.resourceIds.size) {
            throw Exception("Requested storage pairs amount is over the limit")
        }
        val storagePairs = HashMap<ResourceId, Score>()
        while (resourceIds.size < amount) {
            val randomId = this.resourceIds.random()
            if (storagePairs.containsKey(randomId)) {
                continue
            }
            storagePairs[randomId] = Random.nextInt(0, Int.MAX_VALUE)
        }
        return storagePairs
    }


    @Test
    fun testSetAndGetScore() {
        val score: Score = Random.nextInt(0, Int.MAX_VALUE)
        val resourceId = resourceIds.random()
        aggregateScoreStorage.setScore(resourceId, score)
        val retrievedScore = aggregateScoreStorage.getScore(resourceId)
        assertEquals(score, retrievedScore)
    }

    @Test
    fun testSetAndGetValue() {
        val score: Score = Random.nextInt(0, Int.MAX_VALUE)
        val resourceId = resourceIds.random()
        aggregateScoreStorage.setValue(resourceId, score)
        val retrievedScore = aggregateScoreStorage.getValue(resourceId)
        assertEquals(score, retrievedScore)
    }

    @Test
    fun testSetGetAndRemoveScore() {
        val score: Score = Random.nextInt(0, Int.MAX_VALUE)
        val resourceId = resourceIds.random()
        aggregateScoreStorage.setScore(resourceId, score)
        aggregateScoreStorage.remove(resourceId)
        val retrievedScore = aggregateScoreStorage.getScore(resourceId)
        assertEquals(0, retrievedScore)
    }

    @Test
    fun testSetGetAndRemoveValue() {
        val score: Score = Random.nextInt(0, Int.MAX_VALUE)
        val resourceId = resourceIds.random()
        aggregateScoreStorage.setValue(resourceId, score)
        aggregateScoreStorage.remove(resourceId)
        val retrievedScore = aggregateScoreStorage.getValue(resourceId)
        assertEquals(0, retrievedScore)
    }

    @Test
    fun testResetScores() {
        val storagePairs = getRandomStoragePairs(3);
        storagePairs.forEach { entry ->
            aggregateScoreStorage.setScore(entry.key, entry.value)
        }
        val allScores = storagePairs.map { entry -> aggregateScoreStorage.getScore(entry.key) }
        assertEquals(storagePairs.values.toSet(), allScores.toSet())
        aggregateScoreStorage.resetScores(storagePairs.keys.toList())
        val resetScores = storagePairs.map { entry -> aggregateScoreStorage.getScore(entry.key) }
        assertEquals(storagePairs.map { 0 }.toSet(), resetScores.toSet())
    }

    @Test
    fun testResetValues() {
        val storagePairs = getRandomStoragePairs(3);
        storagePairs.forEach { entry ->
            aggregateScoreStorage.setValue(entry.key, entry.value)
        }
        val allScores = storagePairs.map { entry -> aggregateScoreStorage.getValue(entry.key) }
        assertEquals(storagePairs.values.toSet(), allScores.toSet())
        aggregateScoreStorage.resetScores(storagePairs.keys.toList())
        val resetScores = storagePairs.map { entry -> aggregateScoreStorage.getValue(entry.key) }
        assertEquals(storagePairs.map { 0 }.toSet(), resetScores.toSet())
    }

}
