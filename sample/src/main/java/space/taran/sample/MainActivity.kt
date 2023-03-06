package space.taran.sample

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.binding.BindingIndex
import space.taran.arklib.domain.index.ResourcesIndexRepo
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.GeneralPreviewGenerator
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arklib.initArkLib
import space.taran.arklib.initRustLogger
import java.util.ArrayList
import kotlin.io.path.Path

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