package space.taran.arklib.domain.meta

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import space.taran.arklib.ResourceId
import java.lang.IllegalStateException
import java.nio.file.Path

class AggregatedMetadataStorage(
    private val shards: Collection<RootMetadataStorage>,
    private val appScope: CoroutineScope
) : MetadataStorage {

    private val _inProgress = MutableStateFlow(false)

    init {
        initShardsIndexingListener()
    }

    override val updates: Flow<MetadataUpdate> = shards
        .map { it.updates }
        .asIterable()
        .merge()

    override val inProgress = _inProgress.asStateFlow()

    override fun locate(path: Path, id: ResourceId): Result<Metadata> = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            if (it == null) return@let Result.failure(
                IllegalStateException("Shard must be in the aggregation")
            )
            it.locate(path, id)
        }

    override suspend fun forget(id: ResourceId) = shards.forEach {
        it.forget(id)
    }

    private fun initShardsIndexingListener() {
        fun anyShardIndexing() = shards.map { it.inProgress.value }.contains(true)

        shards.forEach { shard ->
            shard.inProgress.onEach {
                _inProgress.emit(anyShardIndexing())
            }.launchIn(appScope)
        }
    }
}
