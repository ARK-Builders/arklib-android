package space.taran.arklib.domain.meta

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
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
        Log.d(METADATA, "Initializing metadata storage for root $root")
        metadataDir.createDirectories()
        initUpdatedResourcesListener()
        initKnownResources()
    }

    override val inProgress = _inProgress.asStateFlow()

    override val updates = _updates.asSharedFlow()

    //todo: add caching
    fun locate(id: ResourceId): Result<Metadata> {
        val metadataPath = metadataPath(id)
        if (!metadataPath.exists()) {
            return Result.failure(
                IllegalStateException("Metadata for $id doesn't exist")
            )
        }

        return readMetadata(metadataPath)
    }

    override fun locate(path: Path, id: ResourceId): Result<Metadata> =
        locate(id)

    override suspend fun forget(id: ResourceId) {
        _updates.emit(MetadataUpdate.Deleted(id))
        metadataPath(id).deleteIfExists()
    }

    private fun generate(resources: Collection<NewResource>) {
        val amount = resources.size
        Log.i(METADATA, "Checking metadata for $amount known resources in $root")

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
        Log.d(METADATA,
            "Generating metadata for resource ${resource.id} by path $path")

        val locator = metadataPath(resource.id)

        MetadataGenerator
            .generate(path, resource)
            .onSuccess { metadata ->
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
            .getOrThrow()
    }

    private fun readMetadata(path: Path): Result<Metadata> {
        Log.d(METADATA, "Reading metadata from $path")

        try {
            val json = Json.parseToJsonElement(path.readText())
            val kind = json.jsonObject[KIND]!!.jsonPrimitive.content

            val metadata = when (Kind.valueOf(kind)) {
                Kind.IMAGE ->
                    Json.decodeFromJsonElement(
                        Metadata.Image.serializer(),
                        json
                    )
                Kind.VIDEO ->
                    Json.decodeFromJsonElement(
                        Metadata.Video.serializer(),
                        json
                    )
                Kind.DOCUMENT ->
                    Json.decodeFromJsonElement(
                        Metadata.Document.serializer(),
                        json
                    )
                Kind.LINK -> Json.decodeFromJsonElement(
                    Metadata.Link.serializer(),
                    json
                )
                Kind.PLAINTEXT -> Json.decodeFromJsonElement(
                    Metadata.PlainText.serializer(),
                    json
                )
                Kind.ARCHIVE -> Json.decodeFromJsonElement(
                    Metadata.Archive.serializer(),
                    json
                )
            }

            return Result.success(metadata)
        } catch (e: SerializationException) {
            return Result.failure(e)
        }
    }

    private fun initUpdatedResourcesListener() {
        Log.d(METADATA, "Listening for updates in the index")
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