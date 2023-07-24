package space.taran.arklib.data.storage

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.taran.arklib.ResourceId
import space.taran.arklib.data.preview.Preview
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

class BitmapStorage(
    private val scope: CoroutineScope,
    private val folderPath: Path
) {
    fun saveBitmap(id: ResourceId, bitmap: Bitmap) {
        Files.createDirectories(folderPath)

        val stream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            Preview.COMPRESSION_QUALITY,
            stream
        )
        Files.write(pathFromId(id), stream.toByteArray())
    }

    fun remove(id: ResourceId) = scope.launch(Dispatchers.IO) {
        pathFromId(id).deleteIfExists()
    }

    private fun pathFromId(id: ResourceId) = folderPath.resolve(id.toString())
}