package space.taran.arknavigator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import space.taran.arklib.initialRustLogger

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        System.loadLibrary("arklib")

        initialRustLogger()
    }
}