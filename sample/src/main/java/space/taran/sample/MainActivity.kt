package space.taran.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.initArkLib
import space.taran.arklib.initRustLogger

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        System.loadLibrary("arklib")
        FoldersRepo.init(application)
        application.initArkLib()
        initRustLogger()
    }
}