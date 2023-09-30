package space.taran.arklib.data.index

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.IndexAggregation
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.index.ResourceUpdates
import dev.arkbuilders.arklib.data.index.RootIndex
import io.mockk.*
import io.mockk.InternalPlatformDsl.toStr
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.*
import kotlin.io.path.Path
import kotlin.random.Random

class IndexAggregationTest {

    private var updatedShards: Int = 0
    private lateinit var shards: Set<RootIndex>
    private lateinit var indexAggregation: IndexAggregation

    @Before
    fun beforeEach() {
        updatedShards = 0
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
        val resourceIdSlot = CapturingSlot<ResourceId>()
        val resourcePaths = createResourceRandomPaths(resources)
        every { rootIndex.allPaths() } returns resourcePaths
        every { rootIndex.allIds() } returns resources.map { it.id }.toSet()
        every { rootIndex.updates } returns MutableSharedFlow<ResourceUpdates>()
        every { rootIndex.allResources() } returns resources.associateBy({ it.id }, { it })
        every { rootIndex.getResource(capture(resourceIdSlot)) } answers {
            val resourceId = resourceIdSlot.captured
            resources.firstOrNull { resource -> resource.id == resourceId }
        }
        every { rootIndex.getPath(capture(resourceIdSlot)) } answers {
            val resourceId = resourceIdSlot.captured
            resourcePaths[resourceId]
        }
        coEvery { rootIndex.updateAll() } answers {
            updatedShards += 1
        }
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

    private fun createResourceRandomPaths(resources: Set<Resource>): Map<ResourceId, Path> {
        return resources.map { resource ->
            val path = Path(UUID.randomUUID().toString())
            resource.id to path
        }.associateBy({ it.first }, { it.second })
    }

    @Test
    fun testAllResources() = runBlocking {
        val expected = shards.map { it.allResources() }.toList().reduce { acc, map -> acc + map }
        val allResources = indexAggregation.allResources()
        assertEquals(9, allResources.size) // 3 resources per shard, so 3 shards * 3 resources
        assertEquals(expected, allResources)
    }

    @Test
    fun testAllResourcesEmpty() = runBlocking {
        val indexAggregation = IndexAggregation(setOf())
        val allResources = indexAggregation.allResources()
        assertEquals(0, allResources.size)
        assertEquals(mapOf<ResourceId, Resource>(), allResources)
    }

    @Test
    fun testGetResource() = runBlocking {
        val shard = shards.random()
        val resource = shard.allResources().values.random()
        val result = indexAggregation.getResource(resource.id)
        assertEquals(resource, result)
    }

    @Test
    fun testGetResourceNotFound() = runBlocking {
        val indexAggregation = IndexAggregation(setOf())
        val result = indexAggregation.getResource(ResourceId(0, 123456789))
        assertNull(result)
    }

    @Test
    fun testGetAllPaths() = runBlocking {
        val expected = shards.map { it.allPaths() }.toList().reduce { acc, map -> acc + map }
        val result = indexAggregation.allPaths()
        assertEquals(9, result.size) // 3 shards * 3 resource per shard
        assertEquals(expected, result)
    }

    @Test
    fun testGetAllPathsEmpty() = runBlocking {
        val indexAggregation = IndexAggregation(listOf())
        val result = indexAggregation.allPaths()
        assertEquals(0, result.size)
        assertEquals(mapOf<ResourceId, Path>(), result)
    }

    @Test
    fun testGetPath() = runBlocking {
        val shard = shards.random()
        val resource = shard.allResources().values.random()
        val result = indexAggregation.getPath(resource.id)
        assertTrue(result is Path)
    }

    @Test
    fun getPathNotFound() = runBlocking {
        val indexAggregation = IndexAggregation(setOf())
        val result = indexAggregation.getResource(ResourceId(0, 123456789))
        assertNull(result)
    }

    @Test
    fun testUpdateAll() = runBlocking {
        indexAggregation.updateAll()
        assertEquals(3, updatedShards)
    }

}
