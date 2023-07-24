package space.taran.arklib.domain.processor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import space.taran.arklib.ResourceId
import java.lang.IllegalStateException

class AggregateProcessor<Value, Update> private constructor(
    private val scope: CoroutineScope,
    private val shards: Collection<RootProcessor<Value, Update>>,
) : Processor<Value, Update>() {

    override val updates: Flow<Update> = shards
        .map { it.updates }
        .asIterable()
        .merge()

    override suspend fun init() {
        shards.forEach { shard ->
            shard.busy.onEach {
                _busy.emit(atLeastOneShardIsBusy())
            }.launchIn(scope)
        }
    }

    override fun retrieve(id: ResourceId): Result<Value> = shards
        .map { it.retrieve(id) }
        .find { it.isSuccess }
        .let {
            if (it == null) {
                return Result.failure(IllegalStateException())
            }

            return it
        }

    override fun forget(id: ResourceId) = shards.forEach {
        it.forget(id)
    }

    private fun atLeastOneShardIsBusy() = shards.map { it.busy.value }.contains(true)

    companion object {
        suspend fun <Value, Update> provide(
            scope: CoroutineScope,
            shards: Collection<RootProcessor<Value, Update>>,
        ) = AggregateProcessor(scope, shards).also {
            it.init()
        }
    }
}