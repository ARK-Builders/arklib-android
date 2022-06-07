package space.taran.arknavigator

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import space.taran.arklib.pdfThumbnailGenerate

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        System.loadLibrary("arklib")
        val fileBytes = resources.openRawResource(R.raw.pdfsample).readBytes()
        val bitmapBytes = pdfThumbnailGenerate(fileBytes)
        val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
        findViewById<ImageView>(R.id.iv).setImageBitmap(bitmap)
    }
}