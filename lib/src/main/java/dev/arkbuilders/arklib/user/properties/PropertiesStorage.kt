package dev.arkbuilders.arklib.user.properties

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.RootIndex
import dev.arkbuilders.arklib.data.storage.AggregateStorage
import dev.arkbuilders.arklib.data.storage.Storage

interface PropertiesStorage: Storage<Properties> {

    fun getProperties(id: ResourceId): Properties =
        getValue(id)

    fun setProperties(id: ResourceId, props: Properties) =
        setValue(id, props)
}

class AggregatePropertiesStorage(
    shards: Collection<Pair<RootPropertiesStorage, RootIndex>>
): AggregateStorage<Properties>(PropertiesMonoid, shards), PropertiesStorage