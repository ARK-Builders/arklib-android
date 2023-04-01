package space.taran.arklib.domain.preview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import java.nio.file.Path

class AggregatedPreviewStorage(
    private val shards: Collection<PlainPreviewStorage>,
    private val appScope: CoroutineScope
) : PreviewStorage {

    private val _indexingFlow = MutableStateFlow(false)

    init {
        initShardsIndexingListener()
    }

    override val inProgress = _indexingFlow.asStateFlow()

    override fun locate(path: Path, resource: Resource): PreviewAndThumbnail? {
        shards.forEach { shard ->
            shard.locate(path, resource)?.let {
                return it
            }
        }
        return null
    }

    override fun forget(id: ResourceId) = shards.forEach {
        it.forget(id)
    }

    override suspend fun store(
        path: Path,
        resource: Resource
    ) = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            require(it != null) { "At least one of shards must yield success" }
            it.store(path, resource)
        }

    private fun initShardsIndexingListener() {
        fun anyShardIndexing() = shards.map { it.inProgress.value }.contains(true)

        shards.forEach { shard ->
            shard.inProgress.onEach {
                _indexingFlow.emit(anyShardIndexing())
            }.launchIn(appScope)
        }
    }
}
