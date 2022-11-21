package space.taran.sample

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.shockwave.pdfium.PdfiumCore
import junit.framework.Assert.*
import org.apache.commons.compress.archivers.zip.ZipUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arklib.PreviewQuality
import space.taran.arklib.pdfPreviewGenerate
import java.io.*
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.io.path.*
import kotlin.system.measureTimeMillis


@RunWith(AndroidJUnit4::class)
class PDFBenchmark {

    @get:Rule
    val mainActivityRule = ActivityTestRule(MainActivity::class.java)
    @Test
    fun benchmark_pdf_bulk() {
        if (true) {
            val appContext = InstrumentationRegistry
                .getInstrumentation()
                .targetContext

            val tarPath = Path("${appContext.cacheDir}/pdf.tar")
            val extractPath = Path("${appContext.cacheDir}/pdfs/")
            appContext.resources.openRawResource(R.raw.pdf).copyTo(
                tarPath.outputStream()
            )
            UnzipUtils.unzip(tarPath, extractPath)
            val files = extractPath.toFile().listFiles()
            val fileNames = arrayOfNulls<String>(files.size)
            files?.mapIndexed { index, item ->
                fileNames[index] = item?.name
            }
            val duration_native = LongArray(files.size)
            val duration_java = LongArray(files.size)
            for (i in files.indices){
                duration_native[i] = measureTimeMillis {gen_pdf_native(appContext, files[i])}
                duration_java[i] = measureTimeMillis { gen_pdf_java(appContext, files[i])}
            }
            println("Files vector: ${fileNames.joinToString()}")
            println("Durations vector(rust): ${duration_native.joinToString { it.toString() }}")
            println("Durations vector(java): ${duration_java.joinToString { it.toString() }}")
            assert(duration_native.sumOf { it } < duration_java.sumOf { it })
        }
    }

    fun gen_pdf_native(ctx: Context, pdfFile: File): Bitmap {
        Log.i("PDFGen","Calling PDF Native Renderer")
        return pdfPreviewGenerate(pdfFile.path, PreviewQuality.LOW)
    }

    private fun gen_pdf_java(ctx: Context, pdfFile: File): Bitmap {
        val page = 0
        val pdfiumCore = PdfiumCore(ctx)
        val fd: ParcelFileDescriptor? =
            ctx
                .contentResolver
                .openFileDescriptor(Uri.fromFile(pdfFile), "r")

        val document = pdfiumCore.newDocument(fd)
        pdfiumCore.openPage(document, page)

        val width = pdfiumCore.getPageWidthPoint(document, page)
        val height = pdfiumCore.getPageHeightPoint(document, page)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        pdfiumCore.renderPageBitmap(document, bitmap, page, 0, 0, width, height)
        pdfiumCore.closeDocument(document)
        return bitmap
    }



    object UnzipUtils {
        @Throws(IOException::class)
        fun unzip(zipFilePath: Path, destDirectory: Path) {
            if (!destDirectory.exists()) destDirectory.createDirectories()

            ZipFile(zipFilePath.absolutePathString()).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val filePath = destDirectory.resolve(entry.name)
                        if (!entry.isDirectory) extractFile(input, filePath)
                        else filePath.createDirectories()
                    }
                }
            }
        }

        @Throws(IOException::class)
        private fun extractFile(inputStream: InputStream, destFilePath: Path) {
            BufferedOutputStream(FileOutputStream(destFilePath.absolutePathString())).use { bos ->
                var read: Int
                val bytesIn = ByteArray(4096)
                while (inputStream.read(bytesIn).also { read = it } != -1) {
                    bos.write(bytesIn, 0, read)
                }
            }
        }
    }
}