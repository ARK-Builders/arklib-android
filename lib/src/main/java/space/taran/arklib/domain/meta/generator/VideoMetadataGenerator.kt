package space.taran.arklib.domain.meta.generator

import android.net.Uri
import space.taran.arklib.app
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.meta.MetadataGenerator
import wseemann.media.FFmpegMediaMetadataRetriever
import java.nio.file.Path

object VideoMetadataGenerator: MetadataGenerator {

    override val acceptedExtensions: Set<String>
        get() = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ts", "mpg")

    override val acceptedMimeTypes: Set<String>
        get() = setOf("video/mp4")

    override fun generate(path: Path, resource: Resource): Result<Metadata> {
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
