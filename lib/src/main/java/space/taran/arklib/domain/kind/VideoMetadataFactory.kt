package space.taran.arklib.domain.kind

import android.net.Uri
import android.util.Log
import space.taran.arklib.app
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.utils.LogTags.PREVIEWS
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.name

object VideoMetadataFactory : MetadataFactory<Metadata.Video> {
    override val acceptedExtensions: Set<String> =
        setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ts", "mpg")
    override val acceptedMimeTypes: Set<String> =
        setOf("video/mp4")
    override val acceptedKindCode = KindCode.VIDEO

    override fun compute(
        path: Path,
        resource: Resource
    ): Metadata.Video {
        val retriever = FFmpegMediaMetadataRetriever()

        try {
            retriever.setDataSource(app, Uri.fromFile(path.toFile()))
        } catch (e: NullPointerException) {
            Log.e(
                PREVIEWS,
                "Failed to setDataSource for ${path.name}"
            )
            throw NullPointerException("no application instance found")
        } catch (e: IllegalArgumentException) {
            Log.e(PREVIEWS, "Failed to setDataSource for ${path.name}")
            throw IOException("failed to read video file")
        }
        val durationMillis = retriever
            .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationMillis?.toLong()

        val width = retriever
            .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toLong()
        val height = retriever
            .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toLong()

        retriever.release()

        return Metadata.Video(height, width, duration)
    }
}
