package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.taran.arklib.ResourceId
import java.lang.IllegalStateException
import java.nio.file.Path

class AggregatedPreviewStorage(
    private val shards: Collection<RootPreviewStorage>,
    private val appScope: CoroutineScope
) : PreviewStorage {

    private val _inProgress = MutableStateFlow(false)

    init {
        initShardsIndexingListener()
    }

    override val inProgress = _inProgress.asStateFlow()

    override suspend fun locate(
        path: Path,
        id: ResourceId): Result<PreviewLocator> =
        shards
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
