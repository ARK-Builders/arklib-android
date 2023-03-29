package space.taran.arklib.domain.meta

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkMetadata
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.kind.GeneralMetadataFactory
import space.taran.arklib.domain.kind.KindCode
import space.taran.arklib.domain.kind.Metadata
import space.taran.arklib.utils.LogTags.METADATA
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PlainMetadataStorage(val root: Path) : MetadataStorage {
    private val metaDir = root.arkFolder().arkMetadata()

    private fun metaPath(id: ResourceId): Path =
        metaDir.resolve(id.toString())

    init {
        metaDir.createDirectories()
    }

    override fun provideMetadata(
        path: Path,
        resource: Resource
    ): Result<Metadata> {
        val metadataPath = metaPath(resource.id)
        if (metadataPath.exists()) {
            readKind(path, metadataPath)
                .onSuccess { return Result.success(it) }
                .onFailure {
                    return generateMetadata(path, resource)
                }
        }

        return generateMetadata(path, resource)
    }

    override fun forget(id: ResourceId) {
        metaPath(id).deleteIfExists()
    }

    private fun generateMetadata(path: Path, resource: Resource): Result<Metadata> {
        val metadataPath = metaPath(resource.id)
        return GeneralMetadataFactory.compute(path, resource)
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
    }

    private fun readKind(path: Path, metadataPath: Path): Result<Metadata> {
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
                    path.readText()
                )
            )
        } catch (e: Exception) {
            Log.w(
                METADATA,
                "Can't read kind with Native Link serializer for $metadataPath"
            )
        }

        return Result.failure(CorruptedKindFile())
    }
}

private class CorruptedKindFile : Exception()

@Serializable
private class NativeLinkJson(val title: String, val desc: String)