package space.taran.arknavigator

import android.content.res.AssetManager
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
import space.taran.arklib.pdfPreviewGenerate;
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.*
import kotlin.io.path.*

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
        Log.i("PDFGen","Calling PDF Native Renderer")

        val data = path.inputStream().readBytes()
        val pdfData = pdfPreviewGenerate(data,PreviewQuality.LOW)
        path.inputStream().close()

        val imgPixs = IntArray(pdfData.width * pdfData.height);
        pdfData.getPixels(imgPixs,0,pdfData.width,0,0,pdfData.width,pdfData.height)

        val picDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        val pic = File(picDir, "test.png")
        Log.i("PDFGen", pic.path)
        val out = FileOutputStream(pic)
        pdfData.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush()
        out.close()

    }
}