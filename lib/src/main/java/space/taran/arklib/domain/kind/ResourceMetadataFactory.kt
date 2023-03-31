package space.taran.arklib.domain.kind

import space.taran.arklib.domain.index.Resource
import space.taran.arklib.utils.extension
import space.taran.arklib.utils.getMimeTypeUsingTika
import java.lang.IllegalArgumentException
import java.nio.file.Path

interface MetadataFactory<T : Metadata> {
    val acceptedExtensions: Set<String>
    val acceptedMimeTypes: Set<String>
    val acceptedKindCode: KindCode
    fun isValid(path: Path) = acceptedExtensions.contains(extension(path))
    fun isValid(mimeType: String) = acceptedMimeTypes.contains(mimeType)
    fun isValid(kindCode: Int) = acceptedKindCode.ordinal == kindCode

    fun compute(path: Path, resource: Resource): T
}

object GeneralMetadataFactory {
    private val factories =
        listOf(
            ImageMetadataFactory,
            VideoMetadataFactory,
            DocumentMetadataFactory,
            PlainTextMetadataFactory,
            ArchiveMetadataFactory
        )

    fun compute(
        path: Path,
        resource: Resource
    ): Result<Metadata> {
        val factory = findFactory(path);
        return if (factory == null) {
            Result.failure(IllegalArgumentException("unknown resource kind"))
        } else {
            Result.success(factory.compute(path, resource))
        }
    }

    private fun findFactory(path: Path): MetadataFactory<Metadata>? {
        var factory = factories.find { it.isValid(path) }
        if (factory != null) return (factory as MetadataFactory<Metadata>)

        if (extension(path).isNotEmpty()) return null
        val mimeType = getMimeTypeUsingTika(path) ?: return null
        factory = factories.find { it.isValid(mimeType) }

        return (factory as MetadataFactory<Metadata>?)
    }
}
