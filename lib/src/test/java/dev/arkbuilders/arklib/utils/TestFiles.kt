package dev.arkbuilders.arklib.utils

import dev.arkbuilders.arklib.data.folders.RootAndFav
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

object TestFiles {

    lateinit var root1: Path
        private set
    lateinit var root2: Path
        private set

    fun rootAndFav1() = RootAndFav(root1.toString(), null)
    fun rootAndFav2() = RootAndFav(root2.toString(), null)
    val allRoots = RootAndFav(null, null)

    fun init() {
        root1 = createTempDirectory()
        root2 = createTempDirectory()
    }

    // 100% unique file name and content
    private val generatedFileContent = mutableSetOf<String>()
    private val generatedFiles = mutableListOf<Path>()
    fun randomFile(parent: Path, extension: String? = null): Path {
        var uuid: String
        do {
            uuid = UUID.randomUUID().toString()
        } while (generatedFileContent.contains(uuid))

        generatedFileContent.add(uuid)
        val fileExt = extension?.let { ".$it" } ?: ""
        val file = parent.resolve("$uuid$fileExt")
        file.createFile()
        file.writeText(uuid)

        generatedFiles.add(file)
        return file
    }

    fun clear() {
        listOf(root1, root2).forEach { it.toFile().deleteRecursively() }
        generatedFiles.forEach { it.deleteIfExists() }
        generatedFiles.clear()
    }
}