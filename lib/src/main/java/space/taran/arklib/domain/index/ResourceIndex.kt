package space.taran.arklib.domain.index

import space.taran.arklib.ResourceId
import java.nio.file.Path

interface ResourceIndex {

    suspend fun allIds(): Set<ResourceId> = allIds(null)

    suspend fun allIds(prefix: Path?): Set<ResourceId> =
        allResources(prefix).map { it.id }.toSet()

    suspend fun allResources(prefix: Path?): Set<Resource>

    suspend fun getResource(id: ResourceId): Resource

    suspend fun getPath(id: ResourceId): Path

    suspend fun updateAll()
}
