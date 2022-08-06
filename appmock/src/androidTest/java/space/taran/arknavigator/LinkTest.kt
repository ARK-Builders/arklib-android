package space.taran.arknavigator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.beust.klaxon.Klaxon
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arklib.createLinkFile
import space.taran.arklib.getLinkHash
import space.taran.arklib.loadLinkFile
import kotlin.io.path.Path
import kotlin.io.path.pathString

data class Time(val secs_since_epoch: Int, val nanos_since_epoch: Int)
data class Link(val title: String, val desc: String, val url: String, val created_time: Time?)


@RunWith(AndroidJUnit4::class)
class LinkTest {
    @get:Rule
    val mainActivityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun link_is_created() {
        val appContext = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val url = "https://example.com/"
        val linkHash = getLinkHash(url)
        val filePath = Path("${appContext.cacheDir}/${linkHash}.link")
        createLinkFile("title", "desc", url, filePath.parent.pathString, false)
        val linkJson = loadLinkFile(filePath.pathString)
        val link = Klaxon().parse<Link>(linkJson)
        assertNotNull(link)
        assertEquals(link?.title, "title")
        assertEquals(link?.desc, "desc")
        assertEquals(link?.url, url)
    }
}