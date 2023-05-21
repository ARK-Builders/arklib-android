package space.taran.arklib.domain.preview

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.app
import space.taran.arklib.domain.storage.FolderStorage
import space.taran.arklib.domain.storage.MonoidIsNotUsed
import java.io.ByteArrayOutputStream
import java.nio.file.Path

internal class BitmapStorage(
    val scope: CoroutineScope, path: Path, logLabel: String
) : FolderStorage<Bitmap>(scope, path, MonoidIsNotUsed(), logLabel) {

    override fun isNeutral(value: Bitmap): Boolean = false

    override suspend fun valueToBinary(value: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()

        value.compress(
            Bitmap.CompressFormat.PNG,
            Preview.COMPRESSION_QUALITY,
            stream
        )

        return stream.toByteArray()
    }

    override suspend fun valueFromBinary(raw: ByteArray): Bitmap {
        return Glide.with(app).asBitmap().load(raw).submit().get()
    }
}