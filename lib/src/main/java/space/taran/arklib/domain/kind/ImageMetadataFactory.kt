package space.taran.arklib.domain.kind

import space.taran.arklib.domain.index.Resource
import java.nio.file.Path

object ImageMetadataFactory : MetadataFactory<Metadata.Image> {
    override val acceptedExtensions: Set<String> =
        setOf("jpg", "jpeg", "png", "svg", "gif", "webp")
    override val acceptedMimeTypes: Set<String>
        get() = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
        )

    override val acceptedKindCode = KindCode.IMAGE

    override fun compute(
        path: Path,
        resource: Resource
    ) = Metadata.Image()
}
