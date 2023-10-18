package dev.arkbuilders.arklib.utils

import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.ImageRequest
import coil.size.Scale
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import dev.arkbuilders.arklib.app
import dev.arkbuilders.arklib.R
import dev.arkbuilders.arklib.ResourceId
import java.nio.file.Path

object ImageUtils {
    private const val MAX_SIZE = 1500
    private const val PREVIEW_SIGNATURE = "preview"
    private const val THUMBNAIL_SIGNATURE = "thumbnail"

    fun iconForExtension(ext: String): Int {
        val drawableID = app.resources
            .getIdentifier(
                "ic_file_$ext",
                "drawable",
                app.packageName
            )

        return if (drawableID > 0) drawableID
        else R.drawable.ic_file
    }

    fun loadImage(
        id: ResourceId,
        image: Path,
        view: ImageView,
        limitSize: Boolean
    ) {
        val signature = "$id$PREVIEW_SIGNATURE"
        view.load(image.toFile(), arkImageLoader) {
            if (limitSize)
                size(MAX_SIZE)
            diskCacheKey(signature)
            memoryCacheKey(signature)
            logListener("[Preview]", id, image)
        }
    }

    fun loadSubsamplingImage(image: Path, view: SubsamplingScaleImageView) {
        view.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        view.setImage(ImageSource.uri(image.toString()))
    }

    fun loadThumbnailWithPlaceholder(
        id: ResourceId,
        image: Path?,
        placeHolder: Int,
        view: ImageView
    ) {
        val signature = "$id$THUMBNAIL_SIGNATURE"

        view.load(image?.toFile(), arkImageLoader) {
            scale(Scale.FILL)
            placeholder(placeHolder)
            diskCacheKey(signature)
            memoryCacheKey(signature)
            crossfade(true)
            logListener("[Thumbnail]", id, image)
        }
    }

    fun ImageRequest.Builder.logListener(
        prefix: String,
        id: ResourceId,
        image: Path?
    ) {
        listener(
            onStart = {
                Log.d(LOG_PREFIX, "$prefix Start loading path[$image] id[$id]")
            },
            onCancel = {
                Log.d(LOG_PREFIX, "$prefix Cancel loading path[$image] id[$id]")
            },
            onError = { _, result ->
                Log.d(
                    LOG_PREFIX,
                    "$prefix Error[${result.throwable}] when loading path[$image] id[$id]"
                )
            },
            onSuccess = { _, result ->
                Log.d(LOG_PREFIX, "$prefix Loaded path[$image] id[$id]")
            },
        )
    }

    val arkImageLoader by lazy {
        ImageLoader.Builder(app)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

}

private const val LOG_PREFIX: String = "[images]"
