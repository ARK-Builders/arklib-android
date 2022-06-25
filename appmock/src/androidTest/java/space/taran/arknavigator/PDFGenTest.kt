package space.taran.arknavigator

import android.content.res.AssetManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arklib.pdfThumbnailGenerate;
import java.io.File
import kotlin.io.path.*

@RunWith(AndroidJUnit4::class)
class PDFGenTest {

    @get:Rule
    val mainActivityRule = ActivityTestRule(MainActivity::class.java)
//    @Test
//    fun multi_pdfgen() {
//        for (i in 0..2) {
//            val appContext = InstrumentationRegistry
//                .getInstrumentation()
//                .targetContext
//            val path = Path("${appContext.cacheDir}/test.pdf")
//            appContext.resources.openRawResource(R.raw.pdfsample).copyTo(
//                path.outputStream()
//            )
//            Log.i("PDFGen","Calling PDF Native Renderer")
//            val data = path.inputStream().readBytes()
//            val pdfData = pdfThumbnailGenerate(data,"")
//            path.inputStream().close()
//        }
//    }
    @Test
    fun is_pdf_gen() {
        val appContext = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val path = Path("${appContext.cacheDir}/test.pdf")
        appContext.resources.openRawResource(R.raw.test).copyTo(
            path.outputStream()
        )
        Log.i("PDFGen","Calling PDF Native Renderer")

        val data = path.inputStream().readBytes()
        val pdfData = pdfThumbnailGenerate(data)
        path.inputStream().close()
    }
}