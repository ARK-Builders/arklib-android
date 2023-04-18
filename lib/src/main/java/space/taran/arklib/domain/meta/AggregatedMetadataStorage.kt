package space.taran.arklib.domain.meta

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import java.nio.file.Path

class AggregatedMetadataStorage(
    private val shards: Collection<PlainMetadataStorage>
) : MetadataStorage {

    //todo: is it used?
    override fun provideMetadata(path: Path, resource: Resource) = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            require(it != null) { "At least one of shards must yield success" }
            it.provideMetadata(path, resource)
        }

    override fun forget(id: ResourceId) = shards.forEach {
        it.forget(id)
    }
}
