package space.taran.arklib.domain.tags

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.storage.AggregateStorage
import space.taran.arklib.domain.storage.Storage

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