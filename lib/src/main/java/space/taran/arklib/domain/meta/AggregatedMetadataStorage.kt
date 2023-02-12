package space.taran.arklib.domain.meta

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.kind.ResourceKind
import space.taran.arklib.domain.meta.MetadataStorage
import java.nio.file.Path

class AggregatedMetadataStorage(
    private val shards: Collection<PlainMetadataStorage>
) : MetadataStorage {

    override fun locateOrGenerateKind(path: Path, meta: ResourceMeta) = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            require(it != null) { "At least one of shards must yield success" }
            it.locateOrGenerateKind(path, meta)
        }

    override fun generateKind(path: Path, meta: ResourceMeta)= shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            require(it != null) { "At least one of shards must yield success" }
            it.generateKind(path, meta)
        }

    override fun forget(id: ResourceId) = shards.forEach {
        it.forget(id)
    }
}
