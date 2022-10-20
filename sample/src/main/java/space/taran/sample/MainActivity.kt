package space.taran.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import space.taran.arklib.initRustLogger
import space.taran.arknavigator.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        System.loadLibrary("arklib")
        initRustLogger()
    }
}