package space.taran.arknavigator

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.junit.*
import java.nio.file.Path
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import space.taran.arklib.index.Difference
import space.taran.arklib.index.ResourceMeta
import space.taran.arklib.index.RustResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.computeId
import java.nio.file.attribute.FileTime
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream
import de.mannodermaus.junit5.ActivityScenarioExtension
import de.mannodermaus.junit5.condition.EnabledIfBuildConfigValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.RegisterExtension
import space.taran.arklib.index.ResourceKind
import java.nio.file.Paths
import kotlin.properties.Delegates

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ResourcesIndexTest() {
    @JvmField
    @RegisterExtension
    val scenarioExtension = ActivityScenarioExtension.launch<MainActivity>()

    private lateinit var ri: RustResourcesIndex
    private lateinit var  tempFilePath: Path
    private var tempId by Delegates.notNull<Long>()
    private var diff: Difference? = null

    private var tempMeta: ResourceMeta? =null
        @BeforeAll
        fun init()  {
            val appCtx = InstrumentationRegistry.getInstrumentation().targetContext

            val rootPath =
                appCtx.cacheDir
            tempFilePath =  Path("/data/data/space.taran.arknavigator/cache/test.pdf")
            Log.i("ResourcesIndex", "Root Path: ${rootPath.path}")
            appCtx.resources.openRawResource(R.raw.test).copyTo(
                tempFilePath.outputStream()
            )
            Log.i("ResourcesIndex", "Copied temp file for testing")
            val tempFileSize = tempFilePath.fileSize()
            Log.i("ResourceIndex", "Computing Id")
            tempId = computeId(tempFileSize,tempFilePath)
            val res = mutableMapOf<Path, ResourceMeta>()
//            res[Path("/test/test")] =
//                ResourceMeta(1,"test","test", FileTime.fromMillis(0),1,ResourceKind.PlainText())
            Log.i("ResourceIndex", "Initializing ri")

             ri = RustResourcesIndex(rootPath.path, res)
        }



    @Test
    @Order(1)
    fun reindex() {
        val difference = ri.reindex()
        diff = difference
        Log.i("Index",difference.added.toString())
        Log.i("Index", difference.added.contains(tempFilePath).toString())
        assertTrue(difference.added.contains(tempFilePath))
    }

    @Test
    @Order(2)
    fun getPath() {
        val path = ri.getPath(tempId)
        assertEquals(path ,tempFilePath)
    }

    @Test
    @Order(3)
    fun getMeta() {
        val meta = ri.getMeta(tempId)
        tempMeta = meta
        assertEquals(meta.id ,tempId)
    }

    @Test
    @Order(4)
    fun listResources() {
        val resMeta = ri.listResources(null)
        Log.i("Index", tempMeta.toString())
        Log.i("Index", resMeta.values.toString())
        assertNotNull(resMeta.values.find { it.id == tempMeta?.id })
    }

    @Test
    @Order(5)
    fun remove() {
        val removed = ri.remove(tempId)
        Log.i("Index", removed.toString())
        val res = ri.listResources("")
        assertFalse(res.keys.contains(removed))
    }

    @Test
    @Order(6)
    fun updateResource() {
        val newMeta = ResourceMeta(1,"","", FileTime.fromMillis(0),1,ResourceKind.PlainText())
        Log.i("Index", tempFilePath.toString())
        ri.updateResource(tempFilePath,newMeta)
        val meta = ri.getMeta(newMeta.id)
        assertTrue(newMeta.id == meta.id)
    }
}
