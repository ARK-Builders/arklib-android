package dev.arkbuilders.arklib.user.tags

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
    private val resourceIds: Set<ResourceId> = setOf(
        ResourceId(1024, 123456789),
        ResourceId(2048, 987654321),
        ResourceId(4096, 246810121),
        ResourceId(8192, 369121518),
    )

    private lateinit var aggregateTagStorage: AggregateTagStorage
    private lateinit var shards: List<Pair<RootTagsStorage, RootIndex>>

    @Before
    fun beforeEach() {
        storedTags.clear()
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
        every { rootIndex.allIds() } returns resourceIds
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
        every { rootTagsStorage.remove(capture(resourceIdSlot)) } answers {
            val resourceId = resourceIdSlot.captured
            storedTags.remove(resourceId)
        }
        return rootTagsStorage;
    }

    @After
    fun afterEach() {
        unmockkAll()
    }

    private fun getRandomStoragePairs(amount: Int): Map<ResourceId, Tags> {
        if (amount > this.resourceIds.size) {
            throw Exception("Requested storage pairs amount is over the limit")
        }
        val storagePairs = HashMap<ResourceId, Tags>()
        while (resourceIds.size < amount) {
            val randomId = this.resourceIds.random()
            if (storagePairs.containsKey(randomId)) {
                continue
            }
            storagePairs[randomId] = createRandomTags(7)
        }
        return storagePairs
    }

    private fun createRandomTags(amount: Int): Set<Tag> {
        return (1..amount).map { UUID.randomUUID().toString() }.toSet()
    }

    @Test
    fun testGetTags() {
        val tags = createRandomTags(7)
        val resourceId = resourceIds.random()
        aggregateTagStorage.setValue(resourceId, tags)
        val retrievedTags = aggregateTagStorage.getTags(resourceId)
        assertEquals(tags, retrievedTags)
    }

    @Test
    fun testGetMultipleTags() {
        val storagePairs = getRandomStoragePairs(3)
        storagePairs.forEach { entry ->
            aggregateTagStorage.setValue(entry.key, entry.value)
        }
        val retrievedTags = aggregateTagStorage.getTags(storagePairs.keys)
        val expected = storagePairs.values.flatten().toSet()
        assertEquals(expected, retrievedTags)
    }

    @Test
    fun testGroupTagsByResources() {
        val storagePairs = getRandomStoragePairs(3)
        storagePairs.forEach { entry ->
            aggregateTagStorage.setValue(entry.key, entry.value)
        }
        val result = aggregateTagStorage.groupTagsByResources(storagePairs.keys)
        assertEquals(storagePairs, result)
    }

    @Test
    fun testRemove() {
        val tags = createRandomTags(7)
        val resourceId = resourceIds.random()
        aggregateTagStorage.setValue(resourceId, tags)
        aggregateTagStorage.remove(resourceId)
        val retrievedTags = aggregateTagStorage.getTags(resourceId)
        assertEquals(emptySet<Tag>(), retrievedTags)
    }

}
