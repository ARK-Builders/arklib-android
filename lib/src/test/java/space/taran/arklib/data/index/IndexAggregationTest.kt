package space.taran.arklib.data.index

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.IndexAggregation
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.index.ResourceUpdates
import dev.arkbuilders.arklib.data.index.RootIndex
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.*
import kotlin.random.Random

class IndexAggregationTest {

    private lateinit var shards: Set<RootIndex>
    private lateinit var indexAggregation: IndexAggregation

    @Before
    fun beforeEach() {
        shards = createShards(3)
        indexAggregation = IndexAggregation(shards)
    }

    @After
    fun afterEach() {
        unmockkAll()
    }

    private fun createShards(amount: Int): Set<RootIndex> {
        val shards = HashSet<RootIndex>(amount)
        for (i in 1..amount) {
            val rootIndex = createRootIndex()
            shards.add(rootIndex)
        }
        return shards
    }

    private fun createRootIndex(): RootIndex {
        val rootIndex = mockk<RootIndex>()
        val resources = createRandomResources(3)
        every { rootIndex.allIds() } returns resources.map { it.id }.toSet()
        every { rootIndex.updates } returns MutableSharedFlow<ResourceUpdates>()
        every { rootIndex.allResources() } returns resources.associateBy({ it.id }, { it })
        return rootIndex;
    }

    private fun createRandomResources(amount: Int): Set<Resource> {
        return (1..amount).map {
            val resourceId = createRandomResourceId()
            Resource(resourceId, UUID.randomUUID().toString(), "txt", FileTime.from(Instant.now()))
        }.toSet()
    }

    private fun createRandomResourceId(): ResourceId {
        val crc32 = generateRandomLong(18)
        val size = Random.nextLong(0, 16364)
        return ResourceId(size, crc32)
    }

    private fun generateRandomLong(length: Int): Long {
        require(length > 0) { "Length must be greater than 0" }
        val stringBuilder = StringBuilder(length)
        repeat(length) {
            val digit = Random.nextInt(1, 10)
            stringBuilder.append(digit.toStr())
        }
        return stringBuilder.toString().toLong()
    }

    @Test
    fun testAllResources() = runBlocking {
        val expected = shards.map { it.allResources() }.toList().reduce { acc, map -> acc + map }
        val allResources = indexAggregation.allResources()
        assertEquals(9, allResources.size) // 3 resources per shard, so 3 shards * 3 resources
        assertEquals(expected, allResources)
    }

}
