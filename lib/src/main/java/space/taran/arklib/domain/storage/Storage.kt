package space.taran.arklib.domain.storage

import space.taran.arklib.ResourceId

interface Storage<V> {
    fun getValue(id: ResourceId): V

    fun setValue(id: ResourceId, value: V)

    fun remove(id: ResourceId)

    suspend fun persist()
}