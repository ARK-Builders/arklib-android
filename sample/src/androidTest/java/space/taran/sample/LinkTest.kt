package space.taran.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.beust.klaxon.Klaxon
import junit.framework.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arklib.createLinkFile
import space.taran.arklib.getLinkHash
import space.taran.arklib.loadLinkFile
import space.taran.arklib.loadLinkPreview
import space.taran.arklib.fetchLinkData
import kotlin.io.path.Path
import kotlin.io.path.pathString

data class Link(val title: String, val desc: String, val url: String)


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
        val rootPath = Path("${appContext.cacheDir}/tmp/")
        val filePath = Path("${appContext.cacheDir}/tmp/dir1/${linkHash}.link")
        for (downloadPreview in listOf<Boolean>(true, false)){
            createLinkFile("title", "desc", url, rootPath, filePath.parent.pathString, downloadPreview)
            val linkJson = loadLinkFile(rootPath.pathString, filePath.pathString)
            val linkPreview = loadLinkPreview(rootPath.pathString, filePath.pathString)
            val link = Klaxon().parse<Link>(linkJson)
            assertNotNull(link)
            assertEquals(link?.title, "title")
            assertEquals(link?.desc, "desc")
            assertEquals(link?.url, url)
            if (downloadPreview){
                assertNotNull(linkPreview)
            }else{
                assertNull(linkPreview)
            }
        }
        val linkData = fetchLinkData(url)
        assertNotNull(linkData)
        assertNotNull(linkData?.title)
        assertFalse(linkData?.title.equals(""))
    }
}