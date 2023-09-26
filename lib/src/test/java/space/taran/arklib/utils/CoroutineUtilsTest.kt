package space.taran.arklib.utils

import dev.arkbuilders.arklib.utils.tickerFlow
import dev.arkbuilders.arklib.utils.tryUnlock
import dev.arkbuilders.arklib.utils.withContextAndLock
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class CoroutineUtilsTest {

    @Test
    fun testWithContextAndLock() = runBlocking {
        val mutex = Mutex()
        var sharedCounter = 0
        val context = EmptyCoroutineContext
        val concurrency = Random.nextInt(2, 5)
        var delayMillis = Random.nextLong(200, 500)
        val elapsedTime = measureTimeMillis {
            val jobs = List(concurrency) {
                launch {
                    withContextAndLock(context, mutex) {
                        delay(delayMillis)
                        sharedCounter += 1
                    }
                }
            }
            jobs.forEach { it.join() }
        }
        println("Elapsed time: $elapsedTime ms")
        assertEquals(concurrency, sharedCounter)
        assertTrue(elapsedTime > concurrency * delayMillis)
    }

    @Test
    fun testTryUnlock() = runBlocking {
        val mutex = Mutex()
        assertFalse(mutex.isLocked)
        mutex.lock()
        assertTrue(mutex.isLocked)
        mutex.tryUnlock()
        assertFalse(mutex.isLocked)
        mutex.tryUnlock()
        assertFalse(mutex.isLocked)
        mutex.tryUnlock()
        assertFalse(mutex.isLocked)
    }

    @Test
    fun testTickerFlow() = runBlocking {
        val delayMillis = Random.nextLong(200, 500)
        val initialDelayMillis = Random.nextLong(200, 500)
        val flow = tickerFlow(delayMillis, initialDelayMillis)
        val collectedValues = mutableListOf<Unit>()
        val job = launch {
            flow.collect { value ->
                collectedValues.add(value)
            }
        }
        delay(initialDelayMillis)
        assertEquals(0, collectedValues.size)
        delay(delayMillis)
        assertEquals(1, collectedValues.size)
        delay(delayMillis * 2)
        assertEquals(3, collectedValues.size)
        job.cancelAndJoin()
        delay(delayMillis * 2)
        assertEquals(3, collectedValues.size)
    }
}
