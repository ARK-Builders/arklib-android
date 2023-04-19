package space.taran.arklib.domain.meta

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkMetadata
import space.taran.arklib.domain.index.NewResource
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.utils.LogTags.METADATA
import java.lang.IllegalStateException
import java.nio.file.Path
import kotlin.io.path.*

class RootMetadataStorage(
    private val index: RootIndex,
    private val appScope: CoroutineScope
) : MetadataStorage {
    val root = index.path

    private val _inProgress = MutableStateFlow(false)
    private val _updates = MutableSharedFlow<MetadataUpdate>()

    private val metadataDir = root.arkFolder().arkMetadata()

    private fun metadataPath(id: ResourceId): Path =
        metadataDir.resolve(id.toString())

    init {
        metadataDir.createDirectories()
        initUpdatedResourcesListener()
        initKnownResources()
    }

    override val inProgress = _inProgress.asStateFlow()

    override val updates = _updates.asSharedFlow()

    override fun locate(path: Path, id: ResourceId): Result<Metadata> =
        locate(id)

    override suspend fun forget(id: ResourceId) {
        _updates.emit(MetadataUpdate.Deleted(id))
        metadataPath(id).deleteIfExists()
    }

    private fun locate(id: ResourceId): Result<Metadata> {
        val metadataPath = metadataPath(id)
        if (!metadataPath.exists()) {
            return Result.failure(
                IllegalStateException("Metadata for $id doesn't exist")
            )
        }

        return readMetadata(metadataPath)
    }

    private fun generate(resources: Collection<NewResource>) {
        appScope.launch(Dispatchers.IO) {
            _inProgress.emit(true)

            val jobs = resources.map { added ->
                launch { generate(added.path, added.resource) }
            }

            jobs.joinAll()
            _inProgress.emit(false)
        }
    }

    private suspend fun generate(path: Path, resource: Resource) {
        require(!path.isDirectory()) { "Folders are not allowed here" }

        val locator = metadataPath(resource.id)

        MetadataGenerator.generate(path, resource)
            .map { metadata ->
                val json = when (metadata) {
                    is Metadata.Archive -> Json.encodeToString(metadata)
                    is Metadata.Document -> Json.encodeToString(metadata)
                    is Metadata.Image -> Json.encodeToString(metadata)
                    is Metadata.Link -> Json.encodeToString(metadata)
                    is Metadata.PlainText -> Json.encodeToString(metadata)
                    is Metadata.Video -> Json.encodeToString(metadata)
                }

                locator.writeText(json)
                _updates.emit(MetadataUpdate.Added(resource.id, path, metadata))
            }
            .onFailure {
                Log.e(METADATA, "Failed to generate metadata for $path")
            }
    }

    private fun readMetadata(path: Path): Result<Metadata> {
        try {
            val jsonElement = Json.parseToJsonElement(path.readText())
            val codeJson = jsonElement.jsonObject["code"]!!.jsonPrimitive.content

            val kind = when (Kind.valueOf(codeJson)) {
                Kind.IMAGE ->
                    Json.decodeFromJsonElement(
                        Metadata.Image.serializer(),
                        jsonElement
                    )
                Kind.VIDEO ->
                    Json.decodeFromJsonElement(
                        Metadata.Video.serializer(),
                        jsonElement
                    )
                Kind.DOCUMENT ->
                    Json.decodeFromJsonElement(
                        Metadata.Document.serializer(),
                        jsonElement
                    )
                Kind.LINK -> Json.decodeFromJsonElement(
                    Metadata.Link.serializer(),
                    jsonElement
                )
                Kind.PLAINTEXT -> Json.decodeFromJsonElement(
                    Metadata.PlainText.serializer(),
                    jsonElement
                )
                Kind.ARCHIVE -> Json.decodeFromJsonElement(
                    Metadata.Archive.serializer(),
                    jsonElement
                )
            }
            return Result.success(kind)
        } catch (e: Exception) {
            Log.w(
                METADATA,
                "Can't read kind with ResourceKind serializer for $path"
            )
        }

        //todo: why do we need this?
        try {
            val nativeLinkJson = Json.decodeFromString(
                NativeLinkJson.serializer(),
                path.readText()
            )
            return Result.success(
                Metadata.Link(
                    nativeLinkJson.title,
                    nativeLinkJson.desc,
                )
            )
        } catch (e: Exception) {
            Log.w(
                METADATA,
                "Can't read kind with Native Link serializer for $path"
            )
        }

        return Result.failure(
            IllegalStateException("Failed to parse metadata from $path")
        )
    }

    private fun initUpdatedResourcesListener() {
        index.updates.onEach { diff ->
            generate(diff.added.values)

            diff.deleted.forEach { (id, _) -> forget(id) }
        }.launchIn(appScope + Dispatchers.IO)
    }

    // this forces to check existing metadata,
    // existing metadata will be pushed into `updates` too,
    // this is needed for checking previews
    private fun initKnownResources() {
        appScope.launch(Dispatchers.IO) {
            generate(index.asAdded())
        }
    }
}

@Serializable
private class NativeLinkJson(val title: String, val desc: String)