package dev.arkbuilders.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arklib.initArkLib
import dev.arkbuilders.arklib.initRustLogger
import dev.arkbuilders.arklib.arkFolder
import dev.arkbuilders.arklib.arkStats
import dev.arkbuilders.arklib.arkFavorites
import dev.arkbuilders.arklib.indexPath
import dev.arkbuilders.arklib.arkProperties
import dev.arkbuilders.arklib.arkMetadata
import dev.arkbuilders.arklib.arkPreviews
import dev.arkbuilders.arklib.arkThumbnails

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        System.loadLibrary("arklib")
        FoldersRepo.init(application)
        application.initArkLib()
        initRustLogger()
        print("Ark: ${arkFolder()}")
        print("Stats: ${arkStats()}")
        print("Favorittes: ${arkFavorites()}")
        print("Index: ${indexPath()}")
        print("Properties: ${arkProperties()}")
        print("Metadata: ${arkMetadata()}")
        print("Previews: ${arkPreviews()}")
        print("Thumbnails: ${arkThumbnails()}")
    }
}