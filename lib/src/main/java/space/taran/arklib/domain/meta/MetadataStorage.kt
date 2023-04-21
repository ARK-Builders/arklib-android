package space.taran.arklib.domain.meta

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import space.taran.arklib.ResourceId
import java.nio.file.Path

interface MetadataStorage {

    val inProgress: StateFlow<Boolean>

    val updates: Flow<MetadataUpdate>

    fun locate(path: Path, id: ResourceId): Result<Metadata>

    suspend fun forget(id: ResourceId)
}


sealed class MetadataUpdate {
    data class Deleted(val id: ResourceId): MetadataUpdate()
    data class Added(val id: ResourceId, val path: Path, val metadata: Metadata): MetadataUpdate()
}