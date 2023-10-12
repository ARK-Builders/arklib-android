package dev.arkbuilders.arklib.user.tags

import dev.arkbuilders.arklib.arkFolder
import dev.arkbuilders.arklib.arkTags
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.arklib.user.tags.Tags
import dev.arkbuilders.arklib.user.tags.TagsStorageRepo
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import dev.arkbuilders.arklib.utils.TestDeps
import dev.arkbuilders.arklib.utils.mockLog
import dev.arkbuilders.arklib.utils.TestFiles
import dev.arkbuilders.arklib.utils.TestRepo
import java.util.UUID
import kotlin.io.path.fileSize

class RootTagStorageTest {

    val root = TestFiles.root1

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

    @Test
    fun persistTest() = runTest(UnconfinedTestDispatcher()) {
        val job = Job()
        val testTagStorage = TagsStorageRepo(
            CoroutineScope(this.coroutineContext + job),
            TestDeps.statsFlow
        ).provide(index)

        val id = index.allIds().first()
        testTagStorage.setTags(id, createRandomTags(3))
        testTagStorage.persist()
        advanceTimeBy(5000L)
        job.cancel()

        assert(root.arkFolder().arkTags().fileSize() > 0)
    }

    private fun createRandomTags(amount: Int): Tags {
        return (1..amount).map { UUID.randomUUID().toString() }.toSet()
    }
}