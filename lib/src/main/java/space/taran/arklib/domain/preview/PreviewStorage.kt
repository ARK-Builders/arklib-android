package space.taran.arklib.domain.preview

import kotlinx.coroutines.flow.StateFlow
import space.taran.arklib.ResourceId
import java.nio.file.Path

interface PreviewStorage {

    val inProgress: StateFlow<Boolean>

    suspend fun locate(path: Path, id: ResourceId): Result<PreviewLocator>

    suspend fun forget(id: ResourceId)

}
