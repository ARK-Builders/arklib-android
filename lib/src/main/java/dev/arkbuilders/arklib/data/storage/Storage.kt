package dev.arkbuilders.arklib.data.storage

import dev.arkbuilders.arklib.ResourceId

interface Storage<V> {
    fun getValue(id: ResourceId): V

    fun setValue(id: ResourceId, value: V)

    fun remove(id: ResourceId)

    suspend fun persist()
}

class StorageException(
    val label: String,
    val msg: String,
    val error: Throwable? = null
) : Exception()