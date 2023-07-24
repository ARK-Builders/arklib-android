package space.taran.arklib.user.properties

import space.taran.arklib.ResourceId
import space.taran.arklib.data.index.RootIndex
import space.taran.arklib.data.storage.AggregateStorage
import space.taran.arklib.data.storage.Storage

interface PropertiesStorage: Storage<Properties> {

    fun getProperties(id: ResourceId): Properties =
        getValue(id)

    fun setProperties(id: ResourceId, props: Properties) =
        setValue(id, props)
}

class AggregatePropertiesStorage(
    shards: Collection<Pair<RootPropertiesStorage, RootIndex>>
): AggregateStorage<Properties>(PropertiesMonoid, shards), PropertiesStorage