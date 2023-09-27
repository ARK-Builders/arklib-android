package space.taran.arklib.user.tags

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.RootIndex
import dev.arkbuilders.arklib.user.tags.AggregateTagStorage
import dev.arkbuilders.arklib.user.tags.RootTagsStorage
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.Tags
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class AggregateTagStorageTest {

    private val storedTags = HashMap<ResourceId, Tags>()
    private val rootIds: List<ResourceId> = listOf(
        ResourceId(1024, 123456789),
        ResourceId(2048, 987654321),
        ResourceId(4096, 246810121),
        ResourceId(8192, 369121518),
    )

    private lateinit var aggregateTagStorage: AggregateTagStorage
    private lateinit var shards: List<Pair<RootTagsStorage, RootIndex>>

    @Before
    fun beforeEach() {
        shards = createShards(3)
        aggregateTagStorage = AggregateTagStorage(shards)
    }

    private fun createShards(amount: Int): List<Pair<RootTagsStorage, RootIndex>> {
        val shards = ArrayList<Pair<RootTagsStorage, RootIndex>>(amount)
        for (i in 1..amount) {
            val rootIndex = createRootIndex()
            val rootTagsStorage = createRootTagsStorage()
            shards.add(rootTagsStorage to rootIndex)
        }
        return shards
    }

    private fun createRootIndex(): RootIndex {
        val rootIndex = mockk<RootIndex>()
        every { rootIndex.allIds() } returns rootIds.toSet()
        return rootIndex;
    }

    private fun createRootTagsStorage(): RootTagsStorage {
        val rootTagsStorage = mockk<RootTagsStorage>()
        val tagsSlot = CapturingSlot<Tags>()
        val resourceIdSlot = CapturingSlot<ResourceId>()
        every { rootTagsStorage.setValue(capture(resourceIdSlot), capture(tagsSlot)) } answers {
            val tags = tagsSlot.captured
            val resourceId = resourceIdSlot.captured
            storedTags[resourceId] = tags
        }
        every { rootTagsStorage.valueById[capture(resourceIdSlot)] } answers {
            val resourceId = resourceIdSlot.captured
            storedTags[resourceId]
        }
        return rootTagsStorage;
    }

    @After
    fun afterEach() {
        unmockkAll()
        storedTags.clear()
    }

    private fun createRandomTags(amount: Int): Set<Tag> {
        return (1..amount).map { UUID.randomUUID().toString() }.toSet()
    }

    @Test
    fun testGetTags() {
        val tags = createRandomTags(7)
        val resourceId = rootIds.random()
        aggregateTagStorage.setValue(resourceId, tags)
        val retrievedTags = aggregateTagStorage.getTags(resourceId)
        assertEquals(tags, retrievedTags)
    }

    @Test
    fun testGetMultipleTags() {
        val resourceIdA = rootIds[0]
        val resourceIdB = rootIds[1]
        val resourceIdC = rootIds[2]
        val tagsA = createRandomTags(7)
        val tagsB = createRandomTags(7)
        val tagsC = createRandomTags(7)
        aggregateTagStorage.setValue(resourceIdA, tagsA)
        aggregateTagStorage.setValue(resourceIdB, tagsB)
        aggregateTagStorage.setValue(resourceIdC, tagsC)
        val retrievedTags = aggregateTagStorage.getTags(setOf(resourceIdA, resourceIdB, resourceIdC))
        assertEquals(tagsA.union(tagsB).union(tagsC), retrievedTags)
    }

    @Test
    fun testGroupTagsByResources() {
        val resourceIdA = rootIds[0]
        val resourceIdB = rootIds[1]
        val resourceIdC = rootIds[2]
        val tagsA = createRandomTags(7)
        val tagsB = createRandomTags(7)
        val tagsC = createRandomTags(7)
        aggregateTagStorage.setValue(resourceIdA, tagsA)
        aggregateTagStorage.setValue(resourceIdB, tagsB)
        aggregateTagStorage.setValue(resourceIdC, tagsC)
        val result = aggregateTagStorage.groupTagsByResources(setOf(resourceIdA, resourceIdB, resourceIdC))
        val expected = mapOf<ResourceId, Tags>(resourceIdA to tagsA, resourceIdB to tagsB, resourceIdC to tagsC)
        assertEquals(expected, result)
    }

}
