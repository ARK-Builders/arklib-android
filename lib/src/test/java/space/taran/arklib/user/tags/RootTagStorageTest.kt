package space.taran.arklib.user.tags

import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.arklib.user.tags.Tags
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import space.taran.arklib.utils.mockLog
import space.taran.arklib.utils.TestFiles
import space.taran.arklib.utils.TestRepo
import java.util.UUID

class RootTagStorageTest {

    companion object {
        lateinit var index: ResourceIndex
        lateinit var tagStorage: TagStorage

        @BeforeClass
        @JvmStatic
        fun before() = runBlocking {
            System.loadLibrary("arklib")
            TestFiles.init()
            mockLog()
            TestFiles.randomFile(TestFiles.root1)
            index = TestRepo.index.provide(TestFiles.root1)
            tagStorage = TestRepo.tags.provide(index)
        }

        @AfterClass
        @JvmStatic
        fun after() {
            TestFiles.clear()
        }
    }

    @Test
    fun getTagsTest() = runTest {
        val id = index.allIds().first()
        val expected = createRandomTags(3)
        tagStorage.setTags(id, expected)
        val actual = tagStorage.getTags(id)
        assertEquals(actual, expected)
    }

    @Test
    fun removeTagTest() {
        val id = index.allIds().first()
        tagStorage.setTags(id, createRandomTags(3))
        tagStorage.remove(id)
        val actual = tagStorage.getTags(id)
        assertEquals(actual, emptySet<Tag>())
    }

    private fun createRandomTags(amount: Int): Tags {
        return (1..amount).map { UUID.randomUUID().toString() }.toSet()
    }
}