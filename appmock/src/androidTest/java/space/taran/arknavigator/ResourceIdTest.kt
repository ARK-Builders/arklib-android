package space.taran.arknavigator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arknavigator.mvp.model.repo.index.computeId
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream

@RunWith(AndroidJUnit4::class)
class ResourceIdTest {
    @get:Rule
    val mainActivityRule = ActivityTestRule(MainActivity::class.java)
    @Test
    fun crc32_iscorrect() {
        val appContext = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val path = Path("${appContext.cacheDir}/lena.jpg")
        appContext.resources.openRawResource(R.raw.lena).copyTo(
            path.outputStream()
        )

        assertEquals(computeId(path.fileSize(), path), 0x342a3d4a)
    }
}