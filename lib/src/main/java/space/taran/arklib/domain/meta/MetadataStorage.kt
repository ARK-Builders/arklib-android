package space.taran.arklib.domain.meta

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.kind.Metadata
import java.nio.file.Path

interface MetadataStorage {

    fun provideMetadata(path: Path, resource: Resource): Result<Metadata>

    fun forget(id: ResourceId)
}
