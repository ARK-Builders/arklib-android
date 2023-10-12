package space.taran.arklib.lib

import android.graphics.Bitmap
import dev.arkbuilders.arklib.PreviewQuality
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.pdfPreviewGenerate
import dev.arkbuilders.arklib.user.tags.TagStorage
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import space.taran.arklib.utils.TestFiles
import space.taran.arklib.utils.TestRepo
import space.taran.arklib.utils.mockLog
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

class PDFGenTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun before() {
            System.loadLibrary("arklib")
        }

        @AfterClass
        @JvmStatic
        fun after() {
            unmockkAll()
            clearAllMocks()
        }
    }

    @Test
    fun testPdfGen() {
        mockkStatic(Bitmap::class)
        mockkStatic(Bitmap.Config::class)
        val bitmap = mockk<Bitmap>()
        val captureWidth = slot<Int>()
        val captureHeight = slot<Int>()
        every {
            Bitmap.createBitmap(
                capture(captureWidth),
                capture(captureHeight),
                any()
            )
        } returns bitmap
        every {
            bitmap.setPixels(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } just runs
        val tempPdfFile = kotlin.io.path.createTempFile()
        // The file is located in lib/test/resources/space/taran/arklib/lib/test.pdf
        PDFGenTest::class.java.getResourceAsStream("test.pdf").use {
            it.copyTo(tempPdfFile.outputStream())
        }
        pdfPreviewGenerate(tempPdfFile.toString(), PreviewQuality.MEDIUM)

        assertEquals(captureWidth.captured, 595)
        assertEquals(captureHeight.captured, 841)

        tempPdfFile.deleteIfExists()
    }
}