package space.taran.arklib.domain.meta

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.kind.ResourceKind
import java.nio.file.Path

interface MetadataStorage {

    fun provideKind(path: Path, meta: ResourceMeta): Result<ResourceKind>

    fun forget(id: ResourceId)
}
