package dev.arkbuilders.arklib.user.tags

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.RootIndex
import dev.arkbuilders.arklib.data.storage.AggregateStorage
import dev.arkbuilders.arklib.data.storage.Storage

interface TagStorage: Storage<Tags> {

    fun getTags(id: ResourceId): Tags =
        getValue(id)

    fun getTags(ids: Iterable<ResourceId>): Tags =
        ids.flatMap { id -> getTags(id) }.toSet()

    fun groupTagsByResources(ids: Iterable<ResourceId>): Map<ResourceId, Tags> =
        ids.map { it to getTags(it) }.toMap()

    fun setTags(id: ResourceId, tags: Tags) =
        setValue(id, tags)
}

class AggregateTagStorage(
    shards: Collection<Pair<RootTagsStorage, RootIndex>>
): AggregateStorage<Tags>(TagsMonoid, shards), TagStorage
