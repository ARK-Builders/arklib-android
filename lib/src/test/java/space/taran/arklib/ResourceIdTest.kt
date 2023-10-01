package space.taran.arklib

import dev.arkbuilders.arklib.computeId
import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlin.io.path.createTempFile
import kotlin.io.path.fileSize
import kotlin.io.path.writeText

class ResourceIdTest {
    @Test
    fun resourceIdTest() {
        System.loadLibrary("arklib")
        val file = createTempFile()
        file.writeText("hello")
        assertEquals(
            computeId(file.fileSize(), file).toString(),
            "5-907060870"
        )
    }
}