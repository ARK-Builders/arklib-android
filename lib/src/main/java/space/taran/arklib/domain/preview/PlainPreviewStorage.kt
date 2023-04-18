package space.taran.arklib.domain.preview

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkPreviews
import space.taran.arklib.arkThumbnails
import space.taran.arklib.domain.index.NewResource
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.index.ResourceUpdates
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.kind.ImageMetadataFactory
import space.taran.arklib.domain.preview.generator.PreviewGenerator
import space.taran.arklib.utils.LogTags.PREVIEWS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

class PlainPreviewStorage(
    private val index: RootIndex,
    private val appScope: CoroutineScope
) : PreviewStorage {
    val root = index.path

    private val previewsDir = root.arkFolder().arkPreviews()
    private val thumbnailsDir = root.arkFolder().arkThumbnails()

    private val _inProgress = MutableStateFlow(false)

    private fun previewPath(id: ResourceId): Path =
        previewsDir.resolve(id.toString())

    private fun thumbnailPath(id: ResourceId): Path =
        thumbnailsDir.resolve(id.toString())

    init {
        previewsDir.createDirectories()
        thumbnailsDir.createDirectories()
        initUpdatedResourcesListener()
        initKnownResources()
    }

    override val inProgress = _inProgress.asStateFlow()

    override fun locate(path: Path, resource: Resource): PreviewAndThumbnail? {
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

        if (ImageMetadataFactory.isValid(path)) {
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

    override suspend fun store(path: Path, resource: Resource) {
        require(!path.isDirectory()) { "Previews for folders are constant" }

        val previewPath = previewPath(resource.id)
        val thumbnailPath = thumbnailPath(resource.id)

        if (ImageMetadataFactory.isValid(path)) {
            if (thumbnailPath.notExists()) {
                GeneralPreviewGenerator.generate(path, previewPath, thumbnailPath)
            }
            return
        }

        if (Files.exists(previewPath)) {
            if (!Files.exists(thumbnailPath)) {
                Log.d(
                    PREVIEWS,
                    "Generating thumbnail for ${resource.id} ($path)"
                )
                val thumbnail =
                    PreviewGenerator.resizePreviewToThumbnail(previewPath)
                PreviewGenerator.storeThumbnail(thumbnailPath, thumbnail)
            }

            return
        }

        Log.d(
            PREVIEWS,
            "Generating preview/thumbnail for ${resource.id} ($path)"
        )
        GeneralPreviewGenerator.generate(path, previewPath, thumbnailPath)
    }

    private fun generate(resources: Collection<NewResource>) {
        appScope.launch(Dispatchers.IO) {
            _inProgress.emit(true)

            val jobs = resources.map { added ->
                launch { store(added.path, added.resource) }
            }

            jobs.joinAll()
            _inProgress.emit(false)
        }
    }

    private fun initUpdatedResourcesListener() {
        index.updates.onEach { diff ->
            generate(diff.added.values)

            diff.deleted.forEach { (id, _) -> forget(id) }
        }.launchIn(appScope + Dispatchers.IO)
    }

    private fun initKnownResources() {
        appScope.launch(Dispatchers.IO) {
            generate(index.asAdded())
        }
    }
}
