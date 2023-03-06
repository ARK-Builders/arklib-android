package space.taran.arklib.domain.meta

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkMetadata
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.kind.GeneralKindFactory
import space.taran.arklib.domain.kind.KindCode
import space.taran.arklib.domain.kind.ResourceKind
import space.taran.arklib.domain.kind.ResourceKindFactory
import space.taran.arklib.utils.LogTags.METADATA
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.createDirectories
import kotlin.io.path.bufferedReader
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

    override fun provideKind(
        path: Path,
        meta: ResourceMeta
    ): Result<ResourceKind> {
        val metadataPath = metaPath(meta.id)
        if (metadataPath.exists()) {
            readKind(path, metadataPath)
                .onSuccess { return Result.success(it) }
                .onFailure {
                    return generateKind(path, meta)
                }
        }

        return generateKind(path, meta)
    }

    override fun forget(id: ResourceId) {
        metaPath(id).deleteIfExists()
    }

    private fun generateKind(path: Path, meta: ResourceMeta): Result<ResourceKind> {
        val metadataPath = metaPath(meta.id)
        val kind = try {
            GeneralKindFactory.fromPath(path, meta)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val jsonKind = when (kind) {
            is ResourceKind.Archive -> Json.encodeToString(kind)
            is ResourceKind.Document -> Json.encodeToString(kind)
            is ResourceKind.Image -> Json.encodeToString(kind)
            is ResourceKind.Link -> Json.encodeToString(kind)
            is ResourceKind.PlainText -> Json.encodeToString(kind)
            is ResourceKind.Video -> Json.encodeToString(kind)
        }

        metadataPath.writeText(jsonKind)
        return Result.success(kind)
    }

    private fun readKind(path: Path, metadataPath: Path): Result<ResourceKind> {
        try {
            val jsonElement = Json.parseToJsonElement(metadataPath.readText())
            val codeJson = jsonElement.jsonObject["code"]!!.jsonPrimitive.content

            val kind = when (KindCode.valueOf(codeJson)) {
                KindCode.IMAGE ->
                    Json.decodeFromJsonElement(
                        ResourceKind.Image.serializer(),
                        jsonElement
                    )
                KindCode.VIDEO ->
                    Json.decodeFromJsonElement(
                        ResourceKind.Video.serializer(),
                        jsonElement
                    )
                KindCode.DOCUMENT ->
                    Json.decodeFromJsonElement(
                        ResourceKind.Document.serializer(),
                        jsonElement
                    )
                KindCode.LINK -> Json.decodeFromJsonElement(
                    ResourceKind.Link.serializer(),
                    jsonElement
                )
                KindCode.PLAINTEXT -> Json.decodeFromJsonElement(
                    ResourceKind.PlainText.serializer(),
                    jsonElement
                )
                KindCode.ARCHIVE -> Json.decodeFromJsonElement(
                    ResourceKind.Archive.serializer(),
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
                ResourceKind.Link(
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