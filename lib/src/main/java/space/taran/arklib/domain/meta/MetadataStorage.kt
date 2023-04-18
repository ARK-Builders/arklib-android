package space.taran.arklib.domain.meta

import kotlinx.coroutines.flow.StateFlow
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.kind.Metadata
import java.nio.file.Path

interface MetadataStorage {

    val inProgress: StateFlow<Boolean>

    fun locate(path: Path, resource: Resource): Result<Metadata>

    fun forget(id: ResourceId)

}
