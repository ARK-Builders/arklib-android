package space.taran.arklib.kind

import android.app.Application
import space.taran.arklib.ResourceId
import space.taran.arklib.dao.ResourceExtra
import space.taran.arklib.index.ResourceMeta
import space.taran.arklib.meta.MetadataStorage
import space.taran.arklib.utils.extension
import space.taran.arklib.utils.getMimeTypeUsingTika
import java.nio.file.Path

interface ResourceKindFactory<T : ResourceKind> {
    val acceptedExtensions: Set<String>
    val acceptedMimeTypes: Set<String>
    val acceptedKindCode: KindCode
    fun isValid(path: Path) = acceptedExtensions.contains(extension(path))
    fun isValid(mimeType: String) = acceptedMimeTypes.contains(mimeType)
    fun isValid(kindCode: Int) = acceptedKindCode.ordinal == kindCode

    fun fromPath(path: Path, meta: ResourceMeta, metadataStorage: MetadataStorage): T
    fun fromRoom(extras: Map<MetaExtraTag, String>): T
    fun toRoom(id: ResourceId, kind: T): Map<MetaExtraTag, String?>
}

object GeneralKindFactory {
    private val factories =
        listOf(
            ImageKindFactory,
            VideoKindFactory,
            DocumentKindFactory,
            LinkKindFactory,
            PlainTextKindFactory,
            ArchiveKindFactory
        )

    fun fromPath(
        path: Path,
        meta: ResourceMeta,
        metadataStorage: MetadataStorage
    ): ResourceKind =
        findFactory(path)?.fromPath(path, meta, metadataStorage)
            ?: error("Factory not found")

    fun fromRoom(
        kindCode: Int?,
        extras: List<ResourceExtra>
    ): ResourceKind? {
        kindCode ?: return null

        val data = extras.associate {
            MetaExtraTag.values()[it.ordinal] to it.value
        }

        return factories.find { factory ->
            factory.isValid(kindCode)
        }?.fromRoom(data) ?: error("Factory not found")
    }

    fun toRoom(id: ResourceId, kind: ResourceKind?): List<ResourceExtra> {
        kind ?: return emptyList()

        val factory = factories.find { factory ->
            factory.isValid(kind.code.ordinal)
        } ?: error("Factory not found")

        return (factory as ResourceKindFactory<ResourceKind>)
            .toRoom(id, kind)
            .filter { entry ->
                entry.value != null
            }.map { entry ->
                ResourceExtra(id, entry.key.ordinal, entry.value!!)
            }
    }

    private fun findFactory(path: Path): ResourceKindFactory<ResourceKind>? {
        var factory = factories.find { it.isValid(path) }
        if (factory != null) return (factory as ResourceKindFactory<ResourceKind>)

        if (extension(path).isNotEmpty()) return null
        val mimeType = getMimeTypeUsingTika(path) ?: return null
        factory = factories.find { it.isValid(mimeType) }

        return (factory as ResourceKindFactory<ResourceKind>?)
    }
}