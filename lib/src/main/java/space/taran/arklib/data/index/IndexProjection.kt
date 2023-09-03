package dev.arkbuilders.arklib.data.index

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import dev.arkbuilders.arklib.ResourceId
import java.nio.file.Path

/**
 * [ResourcePredicate] allows us to define [IndexProjection]
 * in the most generic way. */
typealias ResourcePredicate = (Resource, Path) -> Boolean

/**
 * [IndexProjection] is a type of index that returns ids
 * only for matching resources. Matching can be made
 * either by path or by id.
 *
 * Use-cases:
 * 1. "favorite" folder, which is a subfolder of a "root folder",
 *    can be implemented as [IndexProjection] matching specific
 *    paths.
 * 2. various filters by size or resource kind could be
 *    implemented as [IndexProjection] matching specific
 *    values of `Resource` fields.
 */
class IndexProjection(
    private val root: RootIndex,
    private val predicate: ResourcePredicate): ResourceIndex {

    override val roots: Set<RootIndex> = setOf(root)

    override val updates: Flow<ResourceUpdates> = root.updates
        .mapNotNull { updates ->
            val deleted = updates.deleted.filterValues { lost ->
                predicate(lost.resource, lost.path)
            }

            val added = updates.added.filterValues { new ->
                predicate(new.resource, new.path)
            }

            if (deleted.isEmpty() && added.isEmpty()) {
                return@mapNotNull null
            }

            ResourceUpdates(deleted, added)
        }

    override suspend fun updateAll() = root.updateAll()

    override fun allResources(): Map<ResourceId, Resource> {
        val allPathsMap = root.allPaths()
        return root
            .allResources()
            .filter { (id, res) -> predicate(res, allPathsMap[id]!!)  }
    }

    override fun getResource(id: ResourceId): Resource? {
        val result = getResourceAndPath(id)
        return result?.first
    }

    override fun allPaths(): Map<ResourceId, Path> {
        val allResourcesMap = root.allResources()
        return root
            .allPaths()
            .filter { (id, path) -> predicate(allResourcesMap[id]!!, path) }
    }

    override fun getPath(id: ResourceId): Path? {
        val result = getResourceAndPath(id)
        return result?.second
    }

    private fun getResourceAndPath(id: ResourceId): Pair<Resource, Path>? {
        val path = root.getPath(id)
        val resource = root.getResource(id)

        if (path == null || resource == null) {
            return null
        }

        return resource to path
    }
}