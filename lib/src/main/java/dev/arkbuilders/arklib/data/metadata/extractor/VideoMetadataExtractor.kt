package dev.arkbuilders.arklib.data.metadata.extractor

import android.net.Uri
import dev.arkbuilders.arklib.app
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.metadata.Metadata
import dev.arkbuilders.arklib.data.metadata.MetadataExtractor
import wseemann.media.FFmpegMediaMetadataRetriever
import java.nio.file.Path

object VideoMetadataExtractor: MetadataExtractor {

    override val acceptedExtensions: Set<String>
        get() = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ts", "mpg")

    override val acceptedMimeTypes: Set<String>
        get() = setOf("video/mp4")

    override fun extract(path: Path, resource: Resource): Result<Metadata> {
        val retriever = FFmpegMediaMetadataRetriever()

        val uri = Uri.fromFile(path.toFile())

        try {
            retriever.setDataSource(app, uri)

            val duration = retriever
                .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()

            val width = retriever
                .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toLong()
            val height = retriever
                .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toLong()

            return Result.success(
                Metadata.Video(height, width, duration)
            )
        } catch (e: NullPointerException) {
            return Result.failure(NullPointerException("No application instance found"))
        } catch (e: IllegalArgumentException) {
            return Result.failure(IllegalArgumentException("URI $uri is invalid"))
        } finally {
            retriever.release()
        }
    }
}
