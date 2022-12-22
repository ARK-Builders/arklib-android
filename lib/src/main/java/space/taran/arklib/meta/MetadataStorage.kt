package space.taran.arklib.meta

import space.taran.arklib.ResourceId
import space.taran.arklib.index.ResourceMeta
import java.nio.file.Path

interface MetadataStorage {

    fun locate(path: Path, resource: ResourceMeta): ResourceMeta

    fun forget(id: ResourceId)

    fun generate(path: Path, meta: ResourceMeta)
}
