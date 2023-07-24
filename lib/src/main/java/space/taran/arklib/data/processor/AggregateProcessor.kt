package space.taran.arklib.data.processor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import space.taran.arklib.ResourceId
import space.taran.arklib.data.index.RootIndex

class AggregateProcessor<Value, Update> private constructor(
    private val scope: CoroutineScope,
    private val shards: Collection<Pair<RootProcessor<Value, Update>, RootIndex>>,
) : Processor<Value, Update>() {

    override val updates: Flow<Update> = shards
        .map { (processor, _) -> processor.updates }
        .asIterable()
        .merge()

    override suspend fun init() {
        shards.forEach { (processor, _) ->
            processor.busy.onEach {
                _busy.emit(atLeastOneShardIsBusy())
            }.launchIn(scope)
        }
    }

    override fun retrieve(id: ResourceId): Result<Value> = shards
        .find { (_, index) -> index.allIds().contains(id) }!!
        .let { (processor, _) -> processor.retrieve(id) }


    override fun forget(id: ResourceId) = shards.forEach { (processor, _) ->
        processor.forget(id)
    }

    private fun atLeastOneShardIsBusy() = shards.map { (processor, _) ->
        processor.busy.value
    }.contains(true)

    companion object {
        suspend fun <Value, Update> provide(
            scope: CoroutineScope,
            shards: Collection<Pair<RootProcessor<Value, Update>, RootIndex>>,
        ) = AggregateProcessor(scope, shards).also {
            it.init()
        }
    }
}