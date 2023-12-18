package dev.arkbuilders.arklib.user.tags

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.RootIndex
import dev.arkbuilders.arklib.user.properties.AggregatePropertiesStorage
import dev.arkbuilders.arklib.user.properties.Properties
import dev.arkbuilders.arklib.user.properties.RootPropertiesStorage
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class AggregatePropertiesStorageTest {

    private val storedTags = HashMap<ResourceId, Properties>()
    private val resourceIds: Set<ResourceId> = setOf(
        ResourceId(1024, 123456789),
        ResourceId(2048, 987654321),
        ResourceId(4096, 246810121),
        ResourceId(8192, 369121518),
    )

    private lateinit var propertiesStorage: AggregatePropertiesStorage
    private lateinit var shards: List<Pair<RootPropertiesStorage, RootIndex>>

    @Before
    fun beforeEach() {
        shards = createShards(3)
        propertiesStorage = AggregatePropertiesStorage(shards)
    }

    private fun createShards(amount: Int): List<Pair<RootPropertiesStorage, RootIndex>> {
        val shards = ArrayList<Pair<RootPropertiesStorage, RootIndex>>(amount)
        for (i in 1..amount) {
            val rootIndex = createRootIndex()
            val rootPropsStorage = createRootPropertiesStorage()
            shards.add(rootPropsStorage to rootIndex)
        }
        return shards
    }

    private fun createRootIndex(): RootIndex {
        val rootIndex = mockk<RootIndex>()
        every { rootIndex.allIds() } returns resourceIds
        return rootIndex;
    }

    private fun createRootPropertiesStorage(): RootPropertiesStorage {
        val propertiesSlot = CapturingSlot<Properties>()
        val resourceIdSlot = CapturingSlot<ResourceId>()
        val rootPropsStorage = mockk<RootPropertiesStorage>()
        every { rootPropsStorage.setValue(capture(resourceIdSlot), capture(propertiesSlot)) } answers {
            val properties = propertiesSlot.captured
            val resourceId = resourceIdSlot.captured
            storedTags[resourceId] = properties
        }
        every { rootPropsStorage.valueById[capture(resourceIdSlot)] } answers {
            val resourceId = resourceIdSlot.captured
            storedTags[resourceId]
        }
        return rootPropsStorage;
    }

    @After
    fun afterEach() {
        unmockkAll()
        storedTags.clear()
    }

    private fun getRandomStoragePairs(amount: Int): Map<ResourceId, Properties> {
        if (amount > this.resourceIds.size) {
            throw Exception("Requested storage pairs amount is over the limit")
        }
        val storagePairs = HashMap<ResourceId, Properties>()
        while (resourceIds.size < amount) {
            val randomId = this.resourceIds.random()
            if (storagePairs.containsKey(randomId)) {
                continue
            }
            storagePairs[randomId] = createRandomProperties()
        }
        return storagePairs
    }

    private fun createRandomProperties(): Properties {
        val titles = createRandomStrings(7)
        val descriptions = createRandomStrings(7)
        return Properties(titles, descriptions)
    }

    private fun createRandomStrings(amount: Int): Set<String> {
        return (1..amount).map { UUID.randomUUID().toString() }.toSet()
    }

    @Test
    fun testGetAndSetProperties() {
        val storagePairs = getRandomStoragePairs(3)
        storagePairs.forEach { entry ->
            propertiesStorage.setValue(entry.key, entry.value)
            val retrievedProperties = propertiesStorage.getProperties(entry.key)
            assertEquals(entry.value, retrievedProperties)
        }
    }

}
