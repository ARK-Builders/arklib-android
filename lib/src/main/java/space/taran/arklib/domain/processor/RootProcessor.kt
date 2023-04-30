package space.taran.arklib.domain.processor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class RootProcessor<Value, Update>: Processor<Value, Update>() {
    protected val _updates = MutableSharedFlow<Update>()

    override val updates: Flow<Update> = _updates.asSharedFlow()
}