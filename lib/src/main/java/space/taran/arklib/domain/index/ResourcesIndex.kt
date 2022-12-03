package space.taran.arklib.domain.index

import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

interface ResourcesIndex {

    val kindDetectFailedFlow: Flow<Path>

    // we pass all known resource ids to a storage because
    // 1) any storage exists globally
    // 2) we maintain only 1 storage per root
    // 3) every storage is initialized with resource ids
    suspend fun listIds(prefix: Path?): Set<ResourceIdLegacy> =
        listResources(prefix).map { it.id }.toSet()

    suspend fun listResources(prefix: Path?): Set<ResourceMeta>

    suspend fun listAllIds(): Set<ResourceIdLegacy> = listIds(null)

    // whenever we have an id, we assume that we have this id in the index
    // we must load/calculate all necessary ids before we load presenters
    suspend fun getPath(id: ResourceIdLegacy): Path

    suspend fun getMeta(id: ResourceIdLegacy): ResourceMeta

    suspend fun reindex()

    suspend fun remove(id: ResourceIdLegacy): Path

    suspend fun updateResource(
        oldId: ResourceIdLegacy,
        path: Path,
        newResource: ResourceMeta
    )
}