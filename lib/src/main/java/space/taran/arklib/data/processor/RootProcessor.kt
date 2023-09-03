package dev.arkbuilders.arklib.data.processor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class RootProcessor<V, U>: Processor<V, U>() {
    protected val _updates = MutableSharedFlow<U>()

    override val updates: Flow<U> = _updates.asSharedFlow()
}