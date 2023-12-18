package dev.arkbuilders.arklib.data.preview

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.Job
import dev.arkbuilders.arklib.*
import dev.arkbuilders.arklib.app
import dev.arkbuilders.arklib.data.processor.AggregateProcessor
import dev.arkbuilders.arklib.data.processor.Processor
import dev.arkbuilders.arklib.utils.ImageUtils
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
    val onlyThumbnail: Boolean
) {

    internal companion object {
        const val THUMBNAIL_SIZE = 128
        const val COMPRESSION_QUALITY = 100

        /**
         * See [ImageRequest.Builder.data] for supported data types
         * Note: pass [java.io.File] instead of [java.nio.file.Path]
         */
        suspend fun downscale(resource: Path, data: Any): Result<Bitmap> = runCatching {
            val request = ImageRequest.Builder(app)
                .size(THUMBNAIL_SIZE)
                .precision(Precision.EXACT)
                .scale(Scale.FIT)
                .data(data)
                .listener(
                    onError = { _, result ->
                        Log.d(
                            LOG_PREFIX,
                            "Failed to downscale preview for $resource because ${result.throwable}"
                        )
                    },
                )
                .build()

            ImageUtils.arkImageLoader.execute(request).drawable!!.toBitmap()
        }
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

    fun fullscreen(): Path =
        processor.images[id] ?: previewsDir.resolve(id.toString())

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