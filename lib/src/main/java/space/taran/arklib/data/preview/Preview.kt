package space.taran.arklib.data.preview

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Job
import space.taran.arklib.*
import space.taran.arklib.app
import space.taran.arklib.data.processor.AggregateProcessor
import space.taran.arklib.data.processor.Processor
import space.taran.arklib.utils.ImageUtils
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
    //   * fullscreen image can be absent
    //   * fullscreen image can be the resource itself
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
    private val processor: RootPreviewProcessor,
    private val root: Path,
    private val id: ResourceId,
    private var generateJob: Job? = null,
) {

    private val previewsDir = root.arkFolder().arkPreviews()
    private val thumbnailsDir = root.arkFolder().arkThumbnails()

    fun fullscreen(): Path = processor.images[id] ?: previewsDir.resolve(id.toString())

    fun thumbnail(): Path = thumbnailsDir.resolve(id.toString())

    var status: PreviewStatus = PreviewStatus.ABSENT
        private set

    init {
        check()
    }

    fun isGenerated(): Boolean {
        generateJob?.let {
            return it.isCompleted
        }

        return true
    }

    suspend fun join() {
        generateJob?.join()
        generateJob = null
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

    fun erase() {
        thumbnail().deleteIfExists()

        if (processor.images[id] == null) {
            fullscreen().deleteIfExists()
        }
    }

}

typealias PreviewProcessor = Processor<PreviewLocator, Unit>

typealias AggregatePreviewProcessor = AggregateProcessor<PreviewLocator, Unit>

internal const val LOG_PREFIX: String = "[previews]"