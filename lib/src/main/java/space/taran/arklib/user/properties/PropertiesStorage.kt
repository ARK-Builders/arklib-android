package space.taran.arklib.user.properties

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.storage.AggregateStorage
import space.taran.arklib.domain.storage.Storage
import space.taran.arklib.domain.index.RootIndex

interface PropertiesStorage: Storage<Properties> {

    fun getProperties(id: ResourceId): Properties =
        getValue(id)

    fun setProperties(id: ResourceId, props: Properties) =
        setValue(id, props)
}

class AggregatePropertiesStorage(
    shards: Collection<Pair<RootPropertiesStorage, RootIndex>>
): AggregateStorage<Properties>(PropertiesMonoid, shards), PropertiesStorage