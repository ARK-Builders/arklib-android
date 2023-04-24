package space.taran.arklib.domain.preview

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import space.taran.arklib.*
import space.taran.arklib.app
import space.taran.arklib.utils.ImageUtils
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

data class Preview(
    // if `onlyThumbnail` is `false`:
    //   * the bitmap is fullscreen image
    //   * the bitmap needs to be scaled down
    //     in order to obtain the corresponding thumbnail
    // if `onlyThumbnail` is `true`:
    //   * the bitmap is thumbnail
    //   * there cannot be any fullscreen image
    val bitmap: Bitmap,

    // we don't have fullscreen preview for
    // some kinds of resources, e.g. Image or PlainText
    val onlyThumbnail: Boolean) {

    internal companion object {
        const val THUMBNAIL_SIZE = 128
        const val COMPRESSION_QUALITY = 100

        fun downscale(bitmap: Bitmap): Bitmap =
            downscale(Glide.with(app).asBitmap().load(bitmap))

        fun downscale(builder: RequestBuilder<Bitmap>): Bitmap =
            builder
                .apply(RequestOptions()
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                    .override(THUMBNAIL_SIZE)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                )
                .addListener(ImageUtils.glideExceptionListener<Bitmap>())
                .submit()
                .get()
    }
}

enum class PreviewStatus {
    ABSENT, THUMBNAIL, FULLSCREEN
}

class PreviewLocator(
    private val root: Path,
    private val id: ResourceId,
    private val fullscreen: Path? = null) {

    private val previewsDir = root.arkFolder().arkPreviews()
    private val thumbnailsDir = root.arkFolder().arkThumbnails()

    fun fullscreen(): Path = fullscreen ?: previewsDir.resolve(id.toString())

    fun thumbnail(): Path = thumbnailsDir.resolve(id.toString())

    var status: PreviewStatus = PreviewStatus.ABSENT
        private set

    init {
        check()
    }

    fun check(): PreviewStatus {
        if (!thumbnail().exists()) {
            status = PreviewStatus.ABSENT
            return status
        }

        if (!fullscreen().exists()) {
            status = PreviewStatus.THUMBNAIL
            return status
        }

        status = PreviewStatus.FULLSCREEN
        return status
    }

    fun store(preview: Preview) {
        if (status != PreviewStatus.ABSENT) {
            throw IllegalStateException("Preview already exists")
        }

        if (preview.onlyThumbnail) {
            storeImage(thumbnail(), preview.bitmap)
            return
        }

        storeImage(fullscreen(), preview.bitmap)

        val thumbnail = Preview.downscale(preview.bitmap)
        storeImage(thumbnail(), thumbnail)
    }

    fun erase() {
        thumbnail().deleteIfExists()

        if (fullscreen == null) {
            fullscreen().deleteIfExists()
        }
    }

    private fun storeImage(target: Path, bitmap: Bitmap) {
        Files.newOutputStream(target).use { out ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                Preview.COMPRESSION_QUALITY, out
            )
            out.flush()
        }
    }
}

internal const val LOG_PREFIX: String = "[previews]"