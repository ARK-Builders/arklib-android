package space.taran.arklib.domain.storage

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.taran.arklib.ResourceId
import java.util.concurrent.ConcurrentHashMap

/* A storage is being read from the FS both during application startup
 * and during application lifecycle since it can be changed from outside.
 * We also must persist all changes during application lifecycle into FS. */
abstract class BaseStorage<V>(
    private val label: String,
    private val scope: CoroutineScope,
    private val monoid: Monoid<V>,
): Storage<V> {

    private val logPrefix = "$LOG_PREFIX [$label]"

    internal val valueById: ConcurrentHashMap<ResourceId, V> = ConcurrentHashMap()

    private var initialized = false

    private val persistMutex = Mutex()
    private val persistRequestFlow = MutableSharedFlow<Unit>().also { flow ->
        flow.debounce(PERSIST_INTERVAL).onEach {
            actualPersist()
        }.launchIn(scope)
    }

    internal suspend fun init() {
        if (initialized) {
            throw IllegalStateException("Already initialized")
        }

        if (exists()) {
            readFromDisk { valueById.putAll(it) }

            Log.d(logPrefix, "loaded ${valueById.size} values")
        } else {
            Log.d(logPrefix, "created empty storage")
        }

        initialized = true
        afterInit()
    }

    internal open suspend fun afterInit() {}

    internal suspend fun refresh() {
        syncWithDisk()
    }

    override fun getValue(id: ResourceId) =
        valueById[id] ?: monoid.neutral

    override fun setValue(id: ResourceId, value: V) {
        if (!isNeutral(value)) {
            valueById[id] = value
        } else {
            remove(id)
        }
    }

    override fun remove(id: ResourceId) {
        Log.d(logPrefix, "forgetting resource $id")
        valueById.remove(id)
    }

    override suspend fun persist() {
        persistRequestFlow.emit(Unit)
    }

    private suspend fun actualPersist() = persistMutex.withLock {
        syncWithDisk()
        writeToDisk()
    }

    /* This function is called before each disk write and performs
     * values merging inside if any conflicting modifications
     * came from another peers.
     *
     * For real p2p sync we need to track each peer changes separately,
     * otherwise it is difficult to handle remote deletions properly.
     * At this moment, we only ensure that additions are not lost. */
    private suspend fun syncWithDisk() {
        if (!exists()) {
            // storage could be deleted from outside and, right now,
            // we deal with this case by just re-creating the file,
            // because merging of removals is not that important

            // that also means that we can't drop storage on other device
            // while we have the app running on another one
            // (assume external files synchronization)

            return
        }

        // only resources modified after our read or write
        readFromDisk {
            for (addedId in it.keys - valueById.keys) {
                Log.d(logPrefix, "resource $addedId appeared from outside")
                setValue(addedId, it[addedId]!!)
            }

            for (knownId in it.keys.intersect(valueById.keys)) {
                val theirs = it[knownId]!!
                val ours = getValue(knownId)
                if (theirs != ours) {
                    //if `ours` or `theirs` was `null` they wouldn't conflict
                    require(theirs != null)
                    require(ours != null)

                    setValue(knownId, monoid.combine(ours, theirs))
                }
            }
        }

        if (valueById.any { isNeutral(it.value) }) {
            throw StorageException(label, "Empty value can be an indicator of dirty write")
        }
    }

    /* Reading updated parts of the storage,
     * the timestamp will be updated when `handle` function
     * finished processing new map. */
    protected abstract suspend fun readFromDisk(handle: (Map<ResourceId, V>) -> Unit)

    /* Plain dump of values mapping into filesystem. */
    protected abstract suspend fun writeToDisk(valueById: Map<ResourceId, V>)

    protected abstract fun exists(): Boolean

    protected abstract fun erase()

    protected open fun isNeutral(value: V): Boolean {
        if (value == monoid.neutral) {
            return true
        }

        return false
    }

    private suspend fun writeToDisk() {
        if (valueById.any { isNeutral(it.value) }) {
            throw IllegalStateException("Must not write empty values")
        }

        if (valueById.isEmpty()) {
            if (exists()) {
                Log.d(logPrefix, "no actual data, deleting the file")
                erase()
            }

            return
        }

        writeToDisk(valueById.toMap())
    }

    companion object {
        private const val PERSIST_INTERVAL = 2_000L
    }
}

private const val LOG_PREFIX: String = "[storage]"