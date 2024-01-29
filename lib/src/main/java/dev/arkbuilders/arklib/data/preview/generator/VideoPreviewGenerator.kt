package dev.arkbuilders.arklib.data.preview.generator

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.request.ImageRequest
import dev.arkbuilders.arklib.app
import dev.arkbuilders.arklib.data.metadata.Kind
import dev.arkbuilders.arklib.data.metadata.Metadata
import dev.arkbuilders.arklib.data.preview.Preview
import dev.arkbuilders.arklib.data.preview.PreviewGenerator
import dev.arkbuilders.arklib.utils.ImageUtils
import java.nio.file.Path
import kotlin.io.path.name
import wseemann.media.FFmpegMediaMetadataRetriever

object VideoPreviewGenerator : PreviewGenerator {

    override fun isValid(path: Path, meta: Metadata): Boolean {
        return meta.kind == Kind.VIDEO
    }

    override suspend fun generate(path: Path, meta: Metadata): Result<Preview> =
        generateBitmap(path, (meta as Metadata.Video).duration).map {
            Preview(it, onlyThumbnail = false)
        }

    private suspend fun generateBitmap(
        path: Path,
        durationMillis: Long?
    ): Result<Bitmap> {
        val timeMicros = (durationMillis ?: 10000) / 1000 / 2

        val retriever = FFmpegMediaMetadataRetriever()

        try {
            retriever.setDataSource(app, Uri.fromFile(path.toFile()))
            // Trying 3 ways to get preview image for video.
            // 1. using FFmpegMediaMetadataRetriever
            // 2. using MediaMetadataRetriever
            // 3. using Coil
            val mainBitmap = retriever.frameAtTime ?: let {
                MediaMetadataRetriever().let { mediaMetadataRetriever ->
                    try {
                        mediaMetadataRetriever.setDataSource(
                            app, Uri.fromFile(path.toFile())
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.e(
                            LOG_PREFIX, "Failed to setDataSource for ${path.name}"
                        )
                    }

                    var bitmap: Bitmap? = mediaMetadataRetriever.frameAtTime
                    if (bitmap != null) {
                        val tempBitmap: Bitmap? = mediaMetadataRetriever
                            .getFrameAtTime(
                                timeMicros,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
                        if (tempBitmap != null) {
                            bitmap = tempBitmap
                        }
                    }
                    bitmap
                } ?: let {
                    val request = ImageRequest.Builder(app)
                        .data(path.toFile())
                        .build()

                    ImageUtils.arkImageLoader.execute(request).drawable!!.toBitmap()
                }
            }

            if (mainBitmap != null) {
                val bitmap: Bitmap? = retriever.getFrameAtTime(
                    timeMicros,
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (bitmap != null) {
                    return Result.success(bitmap)
                }
            }
            return Result.success(mainBitmap)
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        } finally {
            retriever.release()
        }
    }
}

private const val LOG_PREFIX: String = "[previews/video]"