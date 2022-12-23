package space.taran.arklib.domain.tags

import space.taran.arklib.domain.index.ResourceIdLegacy

interface TagsStorage {

    fun contains(id: ResourceIdLegacy): Boolean

    fun getTags(id: ResourceIdLegacy): Tags

    fun getTags(ids: Iterable<ResourceIdLegacy>): Tags

    fun groupTagsByResources(ids: Iterable<ResourceIdLegacy>): Map<ResourceIdLegacy, Tags> =
        ids.map { it to getTags(it) }
            .toMap()

    fun setTags(id: ResourceIdLegacy, tags: Tags)

    suspend fun setTagsAndPersist(id: ResourceIdLegacy, tags: Tags)

    suspend fun persist()

    fun listUntaggedResources(): Set<ResourceIdLegacy>

    suspend fun cleanup(existing: Collection<ResourceIdLegacy>)

    suspend fun remove(id: ResourceIdLegacy)
}