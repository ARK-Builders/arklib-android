package dev.arkbuilders.arklib.utils

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.measureTimeMillis

class CoroutineUtilsTest {

    @Test
    fun testWithContextAndLock() = runBlocking {
        val mutex = Mutex()
        val concurrency = 20
        var sharedCounter = 0
        var delayMillis = 300L
        val context = EmptyCoroutineContext
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
        val delayMillis = 300L
        val initialDelayMillis = 500L
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
