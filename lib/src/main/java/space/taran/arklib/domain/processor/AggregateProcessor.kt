package space.taran.arklib.domain.processor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import space.taran.arklib.ResourceId
import java.lang.IllegalStateException

class AggregateProcessor<Value, Update>(
    private val shards: Collection<RootProcessor<Value, Update>>
): Processor<Value, Update>() {

    init {
        fun atLeastOneShardIsBusy() = shards.map { it.busy.value }.contains(true)

        shards.forEach { shard ->
            shard.busy.onEach {
                _busy.emit(atLeastOneShardIsBusy())
            }
        }
    }

    override val updates: Flow<Update> = shards
        .map { it.updates }
        .asIterable()
        .merge()

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
}