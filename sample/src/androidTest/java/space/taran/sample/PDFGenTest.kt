package space.taran.sample

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arklib.PreviewQuality
import space.taran.arklib.pdfPreviewGenerate
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@RunWith(AndroidJUnit4::class)
class PDFGenTest {

    @get:Rule
    val mainActivityRule = ActivityTestRule(MainActivity::class.java)
    @Test
    fun is_pdf_gen() {
        val appContext = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val path = Path("${appContext.cacheDir}/test.pdf")
        appContext.resources.openRawResource(R.raw.test).copyTo(
            path.outputStream()
        )
        Log.i(LOG_PREFIX,"Calling PDF Native Renderer")

        val pdfData = pdfPreviewGenerate(path.toString(), PreviewQuality.LOW)
        path.inputStream().close()

        val imgPixs = IntArray(pdfData.width * pdfData.height);
        pdfData.getPixels(imgPixs,0,pdfData.width,0,0,pdfData.width,pdfData.height)

        val picDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        val pic = File(picDir, "test.png")
        Log.i(LOG_PREFIX, pic.path)
        val out = FileOutputStream(pic)
        pdfData.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush()
        out.close()
    }

}

private const val LOG_PREFIX: String = "[test/pdf]"