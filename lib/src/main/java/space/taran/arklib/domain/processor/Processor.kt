package space.taran.arklib.domain.processor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.taran.arklib.ResourceId

abstract class Processor<Value, Update> {

    abstract val updates: Flow<Update>

    protected val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    abstract fun retrieve(id: ResourceId): Result<Value>

    abstract fun forget(id: ResourceId)
}