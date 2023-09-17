package dev.arkbuilders.arklib.data.index

import dev.arkbuilders.arklib.ResourceId

import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

interface ResourceIndex {

    val roots: Set<RootIndex>

    val updates: Flow<ResourceUpdates>

    suspend fun updateAll()

    suspend fun updateOne(resourcePath: Path, oldId: ResourceId)

    suspend fun updateOne(oldId: ResourceId) = updateOne(
        allPaths()[oldId]!!,
        oldId
    )

    fun allResources(): Map<ResourceId, Resource>

    fun getResource(id: ResourceId): Resource?

    fun allPaths(): Map<ResourceId, Path>

    fun getPath(id: ResourceId): Path?

    fun allIds(): Set<ResourceId> =
        allResources().keys
}


class ResourceUpdates(
    val deleted: Map<ResourceId, LostResource>,
    val added: Map<ResourceId, NewResource>
)

data class LostResource(val path: Path, val resource: Resource)

data class NewResource(val path: Path, val resource: Resource)

internal const val LOG_PREFIX: String = "[index]"