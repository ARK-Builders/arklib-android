package dev.arkbuilders.sample

import android.os.Bundle
import android.util.Log
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
        Log.INFO("Ark: ${arkFolder()}")
        Log.INFO("Stats: ${arkStats()}")
        Log.INFO("Favorites: ${arkFavorites()}")
        Log.INFO("Index: ${indexPath()}")
        Log.INFO("Properties: ${arkProperties()}")
        Log.INFO("Metadata: ${arkMetadata()}")
        Log.INFO("Previews: ${arkPreviews()}")
        Log.INFO("Thumbnails: ${arkThumbnails()}")
    }
}