package space.taran.arklib.domain.index

import space.taran.arklib.ResourceId

import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

interface ResourceIndex {

    val updates: Flow<ResourceUpdates>

    suspend fun updateAll()

    suspend fun allResources(): Set<Resource>

    suspend fun getResource(id: ResourceId): Resource?

    suspend fun getPath(id: ResourceId): Path?

    suspend fun allIds(): Set<ResourceId> =
        allResources().map { it.id }.toSet()
}