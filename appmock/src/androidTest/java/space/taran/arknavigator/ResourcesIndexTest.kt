package space.taran.arknavigator

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import java.nio.file.Path
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import space.taran.arklib.index.Difference
import space.taran.arklib.index.ResourceMeta
import space.taran.arklib.index.RustResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.computeId
import java.nio.file.attribute.FileTime
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream
import java.util.HashMap

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ResourcesIndexTest {

    @get:Rule val mainActivityRule = ActivityTestRule(MainActivity::class.java)

    private val ri: RustResourcesIndex
    private var diff: Difference? = null
    private val tempFileName = "test.pdf"
    private val tempFilePath: Path
    private val tempId: Long
    private var tempMeta: ResourceMeta? =null

    init {
        val appCtx = InstrumentationRegistry.getInstrumentation().targetContext
        val rootPath =
            appCtx.cacheDir
        tempFilePath = Path("${rootPath}/${ tempFileName}")
        appCtx.resources.openRawResource(R.raw.test).copyTo(
            tempFilePath.outputStream()
        )
        Log.i("ResourcesIndex", "Copied temp file for testing")
        val tempFileSize = tempFilePath.fileSize()
        Log.i("ResourceIndex", "Computing Id")
        tempId = computeId(tempFileSize,tempFilePath)
        val res = mutableMapOf<Path, ResourceMeta>()
        Log.i("ResourceIndex", "Initializing ri")

        ri = RustResourcesIndex(rootPath.path, res)
    }


    @Test
    fun case1_reindex() {
        val difference = ri.reindex()
        diff = difference
        assertTrue(difference.added.contains(tempFilePath))
    }
    @Test
    fun case2_getPath() {
        val path = ri.getPath(tempId)
        assertEquals(path ,tempFilePath)
    }

    @Test
    fun case3_getMeta() {
        val meta = ri.getMeta(tempId)
        tempMeta = meta
        assertEquals(meta.id ,tempId)
    }

    @Test
    fun case4_listResources() {
        val resMeta = ri.listResources("")
        assertTrue(resMeta.values.contains(tempMeta))
    }

    @Test
    fun case5_remove() {
        val removed = ri.remove(tempId)
        Log.i("Index", removed.toString())
        val res = ri.listResources("")
        assertTrue(res.keys.contains(removed))
    }

    @Test
    fun case6_updateResource() {
        val newMeta = ResourceMeta(1,"","", FileTime.fromMillis(0),1,null)
        ri.updateResource(Path(""),newMeta)
        val meta = ri.getMeta(tempId)
        assertTrue(newMeta == meta)
    }
}
