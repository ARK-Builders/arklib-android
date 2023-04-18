package space.taran.arklib.domain.meta

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import space.taran.arklib.domain.kind.GeneralMetadataFactory
import space.taran.arklib.domain.kind.KindCode
import space.taran.arklib.domain.kind.Metadata
import space.taran.arklib.utils.LogTags.METADATA
import java.lang.IllegalStateException
import java.nio.file.Path
import kotlin.io.path.*

class PlainMetadataStorage(
    private val index: RootIndex,
    private val appScope: CoroutineScope
) : MetadataStorage {
    val root = index.path

    private val metadataDir = root.arkFolder().arkMetadata()

    private val _inProgress = MutableStateFlow(false)

    private fun metadataPath(id: ResourceId): Path =
        metadataDir.resolve(id.toString())

    init {
        metadataDir.createDirectories()
        initUpdatedResourcesListener()
        initKnownResources()
    }

    override val inProgress = _inProgress.asStateFlow()

    override fun locate(path: Path, resource: Resource): Result<Metadata> {
        val metadataPath = metadataPath(resource.id)
        if (!metadataPath.exists()) {
            return Result.failure(
                IllegalStateException("Metadata for $path doesn't exist")
            )
        }

        return readKind(metadataPath)
    }

    override fun forget(id: ResourceId) {
        metadataPath(id).deleteIfExists()
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

    private fun generate(path: Path, resource: Resource) {
        require(!path.isDirectory()) { "Folders are not allowed here" }

        val metadataPath = metadataPath(resource.id)

        GeneralMetadataFactory.compute(path, resource)
            .onSuccess { metadata ->
                val jsonKind = when (metadata) {
                    is Metadata.Archive -> Json.encodeToString(metadata)
                    is Metadata.Document -> Json.encodeToString(metadata)
                    is Metadata.Image -> Json.encodeToString(metadata)
                    is Metadata.Link -> Json.encodeToString(metadata)
                    is Metadata.PlainText -> Json.encodeToString(metadata)
                    is Metadata.Video -> Json.encodeToString(metadata)
                }

                metadataPath.writeText(jsonKind)
            }
            .onFailure {
                Log.e(
                    METADATA,
                    "Failed to generate metadata for $path"
                )
            }
    }

    private fun readKind(metadataPath: Path): Result<Metadata> {
        try {
            val jsonElement = Json.parseToJsonElement(metadataPath.readText())
            val codeJson = jsonElement.jsonObject["code"]!!.jsonPrimitive.content

            val kind = when (KindCode.valueOf(codeJson)) {
                KindCode.IMAGE ->
                    Json.decodeFromJsonElement(
                        Metadata.Image.serializer(),
                        jsonElement
                    )
                KindCode.VIDEO ->
                    Json.decodeFromJsonElement(
                        Metadata.Video.serializer(),
                        jsonElement
                    )
                KindCode.DOCUMENT ->
                    Json.decodeFromJsonElement(
                        Metadata.Document.serializer(),
                        jsonElement
                    )
                KindCode.LINK -> Json.decodeFromJsonElement(
                    Metadata.Link.serializer(),
                    jsonElement
                )
                KindCode.PLAINTEXT -> Json.decodeFromJsonElement(
                    Metadata.PlainText.serializer(),
                    jsonElement
                )
                KindCode.ARCHIVE -> Json.decodeFromJsonElement(
                    Metadata.Archive.serializer(),
                    jsonElement
                )
            }
            return Result.success(kind)
        } catch (e: Exception) {
            Log.w(
                METADATA,
                "Can't read kind with ResourceKind serializer for $metadataPath"
            )
        }
        try {
            val nativeLinkJson = Json.decodeFromString(
                NativeLinkJson.serializer(),
                metadataPath.readText()
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
                "Can't read kind with Native Link serializer for $metadataPath"
            )
        }

        return Result.failure(
            IllegalStateException("Failed to parse metadata from $metadataPath")
        )
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

@Serializable
private class NativeLinkJson(val title: String, val desc: String)