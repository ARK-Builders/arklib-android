package space.taran.arklib.domain.preview

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import java.nio.file.Path

interface PreviewStorage {

    fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail?

    fun forget(id: ResourceId)

    fun store(path: Path, meta: ResourceMeta)
}
