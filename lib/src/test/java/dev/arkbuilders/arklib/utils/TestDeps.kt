package dev.arkbuilders.arklib.utils

import dev.arkbuilders.arklib.data.stats.StatsEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope

object TestDeps {
    val scope = TestScope()
    val statsFlow = MutableSharedFlow<StatsEvent>()
}