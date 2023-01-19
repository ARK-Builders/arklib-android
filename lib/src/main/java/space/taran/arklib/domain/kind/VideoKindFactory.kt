package space.taran.arklib.domain.kind

import android.net.Uri
import android.util.Log
import space.taran.arklib.ResourceId
import space.taran.arklib.app
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.meta.MetadataStorage
import space.taran.arklib.utils.LogTags.PREVIEWS
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.name

object VideoKindFactory : ResourceKindFactory<ResourceKind.Video> {
    override val acceptedExtensions: Set<String> =
        setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ts", "mpg")
    override val acceptedMimeTypes: Set<String> =
        setOf("video/mp4")
    override val acceptedKindCode = KindCode.VIDEO

    override fun fromPath(path: Path,
                          meta: ResourceMeta,
                          metadataStorage: MetadataStorage
    ): ResourceKind.Video {
        val retriever = FFmpegMediaMetadataRetriever()

        try {
            retriever.setDataSource(app, Uri.fromFile(path.toFile()))
        }
        catch (e: NullPointerException) {
            Log.e(
                PREVIEWS,
                "Failed to setDataSource for ${path.name}"
            )
            throw NullPointerException("no application instance found")
        }
        catch (e: IllegalArgumentException) {
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

        return ResourceKind.Video(height, width, duration)
    }

    override fun fromRoom(extras: Map<MetaExtraTag, String>): ResourceKind.Video =
        ResourceKind.Video(
            extras[MetaExtraTag.HEIGHT]?.toLong(),
            extras[MetaExtraTag.WIDTH]?.toLong(),
            extras[MetaExtraTag.DURATION]?.toLong()
        )

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Video
    ): Map<MetaExtraTag, String?> =
        mapOf(
            MetaExtraTag.HEIGHT to kind.height?.toString(),
            MetaExtraTag.WIDTH to kind.width?.toString(),
            MetaExtraTag.DURATION to kind.duration?.toString()
        )
}
