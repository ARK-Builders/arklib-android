package space.taran.arklib.preview

import space.taran.arklib.ResourceId
import space.taran.arklib.index.ResourceMeta
import java.nio.file.Path

interface PreviewStorage {

    fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail?

    fun forget(id: ResourceId)

    fun store(path: Path, meta: ResourceMeta)
}
