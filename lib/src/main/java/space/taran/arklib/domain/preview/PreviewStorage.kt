package space.taran.arklib.domain.preview

import kotlinx.coroutines.flow.StateFlow
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import java.nio.file.Path

interface PreviewStorage {

    val indexingFlow: StateFlow<Boolean>

    fun locate(path: Path, resource: Resource): PreviewAndThumbnail?

    fun forget(id: ResourceId)

    suspend fun store(path: Path, resource: Resource)
}
