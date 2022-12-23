package space.taran.arklib.domain.meta

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import java.nio.file.Path

interface MetadataStorage {

    fun locate(path: Path, resource: ResourceMeta): ResourceMeta

    fun forget(id: ResourceId)

    fun generate(path: Path, meta: ResourceMeta)
}
