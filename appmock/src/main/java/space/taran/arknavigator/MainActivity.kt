package space.taran.arknavigator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import space.taran.arknavigator.mvp.model.repo.index.computeId
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        System.loadLibrary("arklib")

    }
}