package space.taran.arklib.domain.storage

import space.taran.arklib.ResourceId

open class AggregateStorage<V>(
    private val monoid: Monoid<V>,
    private val shards: Collection<BaseStorage<V>>
): Storage<V> {

    override fun getValue(id: ResourceId): V =
        shards.map { it.valueById[id] }
            .find { it != null }
            ?: monoid.neutral

    override fun setValue(id: ResourceId, value: V) =
        shards.forEach { it.setValue(id, value) }

    override fun remove(id: ResourceId) =
        shards.forEach { it.remove(id) }

    override fun refresh() =
        shards.forEach { it.refresh() }

    override fun persist() =
        shards.forEach { it.persist() }
}