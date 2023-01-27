package space.taran.arklib.domain.preview

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkPreviews
import space.taran.arklib.arkThumbnails
import space.taran.arklib.domain.index.PlainResourcesIndex
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.kind.ImageKindFactory
import space.taran.arklib.domain.preview.generator.PreviewGenerator
import space.taran.arklib.utils.LogTags.PREVIEWS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

class PlainPreviewStorage(
    val root: Path,
    private val index: PlainResourcesIndex,
    private val appScope: CoroutineScope
) : PreviewStorage {
    private val previewsDir = root.arkFolder().arkPreviews()
    private val thumbnailsDir = root.arkFolder().arkThumbnails()

    private fun previewPath(id: ResourceId): Path =
        previewsDir.resolve(id.toString())

    private fun thumbnailPath(id: ResourceId): Path =
        thumbnailsDir.resolve(id.toString())

    init {
        previewsDir.createDirectories()
        thumbnailsDir.createDirectories()
        index.resourceDiffFlow.onEach { diff ->
            appScope.launch(Dispatchers.IO) {
                diff.added.forEach { (meta, path) ->
                    launch { store(path, meta) }
                }
                diff.deleted.forEach { (meta, _) -> forget(meta.id) }
            }
        }.launchIn(appScope + Dispatchers.IO)

    }

    override fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail? {
        val preview = previewPath(resource.id)
        val thumbnail = thumbnailPath(resource.id)
        if (!Files.exists(thumbnail)) {
            Log.w(PREVIEWS, "thumbnail was not found for resource $resource")
            if (Files.exists(preview)) {
                Log.e(
                    PREVIEWS,
                    "Preview exists but thumbnail doesn't for resource $resource"
                )
            }

            return null
        }

        if (ImageKindFactory.isValid(path)) {
            return PreviewAndThumbnail(
                preview = path, // using the resource itself as its preview
                thumbnail = thumbnail
            )
        }

        return PreviewAndThumbnail(
            preview = preview,
            thumbnail = thumbnail
        )
    }

    override fun forget(id: ResourceId) {
        previewPath(id).deleteIfExists()
        thumbnailPath(id).deleteIfExists()
    }

    override suspend fun store(path: Path, meta: ResourceMeta) {
        require(!path.isDirectory()) { "Previews for folders are constant" }

        val previewPath = previewPath(meta.id)
        val thumbnailPath = thumbnailPath(meta.id)

        if (ImageKindFactory.isValid(path)) {
            if (thumbnailPath.notExists()) {
                GeneralPreviewGenerator.generate(path, previewPath, thumbnailPath)
            }
            return
        }

        if (Files.exists(previewPath)) {
            if (!Files.exists(thumbnailPath)) {
                Log.d(
                    PREVIEWS,
                    "Generating thumbnail for ${meta.id} ($path)"
                )
                val thumbnail =
                    PreviewGenerator.resizePreviewToThumbnail(previewPath)
                PreviewGenerator.storeThumbnail(thumbnailPath, thumbnail)
            }

            return
        }

        Log.d(
            PREVIEWS,
            "Generating preview/thumbnail for ${meta.id} ($path)"
        )
        GeneralPreviewGenerator.generate(path, previewPath, thumbnailPath)
    }
}
