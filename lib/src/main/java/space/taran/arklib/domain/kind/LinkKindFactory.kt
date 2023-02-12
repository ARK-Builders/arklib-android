package space.taran.arklib.domain.kind

import kotlinx.serialization.Serializable
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.meta.MetadataStorage
import java.nio.file.Path
import kotlin.io.path.readText

object LinkKindFactory : ResourceKindFactory<ResourceKind.Link> {
    override val acceptedExtensions = setOf("link")
    override val acceptedKindCode = KindCode.LINK
    override val acceptedMimeTypes: Set<String>
        get() = setOf()

    override fun fromPath(
        path: Path,
        meta: ResourceMeta
    ): ResourceKind.Link {
        return ResourceKind.Link()
    }
}
