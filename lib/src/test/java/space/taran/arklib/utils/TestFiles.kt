package space.taran.arklib.utils

import space.taran.arkfilepicker.folders.RootAndFav
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

object TestFiles {

    val root1 = createTempDirectory()
    val root2 = createTempDirectory()

    val rootAndFav1 = RootAndFav(root1.toString(), null)
    val rootAndFav2 = RootAndFav(root2.toString(), null)
    val allRoots = RootAndFav(null, null)

    // 100% unique file name and content
    private val generatedFileContent = mutableSetOf<String>()
    fun randomFile(parent: Path, extension: String = ""): Path {
        var uuid: String
        do {
            uuid = UUID.randomUUID().toString()
        } while (generatedFileContent.contains(uuid))

        generatedFileContent.add(uuid)
        val file = parent.resolve("$uuid.$extension")
        file.createFile()
        file.writeText(uuid)
        return file
    }
}