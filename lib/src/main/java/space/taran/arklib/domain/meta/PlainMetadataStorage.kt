package space.taran.arklib.domain.meta

import android.util.Log
import kotlinx.serialization.json.Json
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkMetadata
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.kind.GeneralKindFactory
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

    override fun locateOrGenerateKind(
        path: Path,
        meta: ResourceMeta
    ): Result<ResourceKind> {
        val metadataPath = metaPath(meta.id)
        if (metadataPath.exists()) {
            readKind(metadataPath)
                .onSuccess { return Result.success(it) }
                .onFailure {
                    metadataPath.deleteIfExists()
                    return generateKind(path, meta)
                }
        }

        return generateKind(path, meta)
    }

    override fun generateKind(path: Path, meta: ResourceMeta): Result<ResourceKind> {
        val metadataPath = metaPath(meta.id)
        return try {
            val kind = GeneralKindFactory.fromPath(path, meta)
            metadataPath.writeText(
                Json.encodeToString(
                    ResourceKind.serializer(),
                    kind
                )
            )
            Result.success(kind)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun forget(id: ResourceId) {
        metaPath(id).deleteIfExists()
    }

    private fun readKind(metadataPath: Path): Result<ResourceKind> {
        try {
            val kind = Json.decodeFromString(
                ResourceKind.serializer(),
                metadataPath.readText()
            )
            return Result.success(kind)
        } catch (e: Exception) {
            Log.w(
                METADATA,
                "Can't parse file[$metadataPath] with ResourceKind serializer "
            )
        }
        try {
            val kind = Json.decodeFromString(
                ResourceKind.Link.serializer(),
                metadataPath.readText()
            )
            return Result.success(kind)
        } catch (e: Exception) {
            Log.w(METADATA, "Can't parse file[$metadataPath] with Link serializer ")
        }

        return Result.failure(CorruptedKindFile())
    }
}

private class CorruptedKindFile : Exception()
