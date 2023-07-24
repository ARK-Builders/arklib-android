package space.taran.arklib.data.storage

import space.taran.arklib.ResourceId
import space.taran.arklib.data.index.RootIndex

open class AggregateStorage<V>(
    private val monoid: Monoid<V>,
    private val shards: Collection<Pair<BaseStorage<V>, RootIndex>>
) : Storage<V> {

    override fun getValue(id: ResourceId): V =
        shards.firstNotNullOfOrNull { (storage, _) ->
            storage.valueById[id]
        } ?: monoid.neutral

    override fun setValue(id: ResourceId, value: V) =
        shards.forEach { (storage, index) ->
            if (index.allIds().contains(id))
                storage.setValue(id, value)
        }

    override fun remove(id: ResourceId) =
        shards.forEach { (storage, _) -> storage.remove(id) }

    override suspend fun persist() =
        shards.forEach { (storage, _) -> storage.persist() }
}