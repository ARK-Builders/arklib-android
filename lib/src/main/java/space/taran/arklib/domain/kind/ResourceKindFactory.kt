package space.taran.arklib.domain.kind

import space.taran.arklib.domain.index.ResourceMeta
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

    fun fromPath(path: Path, meta: ResourceMeta): T
}

object GeneralKindFactory {
    private val factories =
        listOf(
            ImageKindFactory,
            VideoKindFactory,
            DocumentKindFactory,
            PlainTextKindFactory,
            ArchiveKindFactory
        )

    fun fromPath(
        path: Path,
        meta: ResourceMeta
    ): ResourceKind =
        findFactory(path)?.fromPath(path, meta) ?: error("Factory not found")

    private fun findFactory(path: Path): ResourceKindFactory<ResourceKind>? {
        var factory = factories.find { it.isValid(path) }
        if (factory != null) return (factory as ResourceKindFactory<ResourceKind>)

        if (extension(path).isNotEmpty()) return null
        val mimeType = getMimeTypeUsingTika(path) ?: return null
        factory = factories.find { it.isValid(mimeType) }

        return (factory as ResourceKindFactory<ResourceKind>?)
    }
}
