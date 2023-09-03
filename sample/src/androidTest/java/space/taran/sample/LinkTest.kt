package dev.arkbuilders.sample

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import de.mannodermaus.junit5.ActivityScenarioExtension
import junit.framework.Assert.assertNotNull
import org.junit.Rule
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.RegisterExtension
import dev.arkbuilders.arklib.createLinkFile
import dev.arkbuilders.arklib.fetchLinkData
import dev.arkbuilders.arklib.getLinkHash
import dev.arkbuilders.arklib.loadLinkFile
import dev.arkbuilders.sample.GrantPermissionExtension.Companion.grant
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class LinkTest {
    @JvmField
    @RegisterExtension
    val scenarioExtension = ActivityScenarioExtension.launch<MainActivity>()

    @RegisterExtension
    var permissionExtension = grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    val url = "https://github.com/ARK-Builders/arklib-android"

    @Test
    @Order(1)
    fun linkIsCreated() {
        val root = getRootPath()

        createLinkFile(
            root,
            title = "",
            desc = "",
            url,
            root,
            downloadPreview = true
        )

        val linkId = getLinkHash(url)
        assert(root.resolve("$linkId.link").exists())
        assert(root.resolve(".ark").resolve("meta").resolve(linkId).exists())
        assert(root.resolve(".ark").resolve("previews").resolve(linkId).exists())
    }

    @Test
    @Order(2)
    fun linkIsFetched() {
        val linkData = fetchLinkData(url)!!
        assertNotNull(linkData.url)
        assertNotNull(linkData.title)
        assertNotNull(linkData.desc)
        assertNotNull(linkData.imageUrl)
    }

    @Test
    @Order(3)
    fun linkFileIsLoaded() {
        val root = getRootPath()
        val linkId = getLinkHash(url)
        val linkData = loadLinkFile(root, root.resolve("$linkId.link"))
        assertNotNull(linkData.url)
        assertNotNull(linkData.title)
        assertNotNull(linkData.desc)
    }

    private fun getRootPath(): Path {
        val appContext = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val device = appContext.getExternalFilesDirs(null)
            .toList()
            .filterNotNull()
            .filter { it.exists() }
            .map {
                it.toPath().toRealPath()
                    .takeWhile { part ->
                        part != Path("Android")
                    }
                    .fold(Path("/")) { parent, child ->
                        parent.resolve(child)
                    }
            }
            .first()

        return device.resolve("testRoot")
    }
}