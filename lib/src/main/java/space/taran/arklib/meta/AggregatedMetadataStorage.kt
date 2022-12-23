package space.taran.arklib.meta

import space.taran.arklib.ResourceId
import space.taran.arklib.index.ResourceMeta
import java.nio.file.Path

class AggregatedMetadataStorage(
    private val shards: Collection<PlainMetadataStorage>
) : MetadataStorage {

    override fun locate(path: Path, resource: ResourceMeta): ResourceMeta {
        shards.forEach { shard ->
            shard.locate(path, resource).let {
                return it
            }
        }
        return resource
    }

    override fun forget(id: ResourceId) = shards.forEach {
        it.forget(id)
    }

    override fun generate(
        path: Path,
        meta: ResourceMeta
    ) = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            require(it != null) { "At least one of shards must yield success" }
            it.generate(path, meta)
        }
}