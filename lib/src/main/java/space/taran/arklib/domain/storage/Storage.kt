package space.taran.arklib.domain.storage

import android.util.Log
import space.taran.arklib.ResourceId

/* A storage is being read from the FS both during application startup
 * and during application lifecycle since it can be changed from outside.
 * We also must persist all changes during application lifecycle into FS. */
abstract class Storage<V>(
    private val monoid: Monoid<V>,
    logLabel: String) {

    private val label = "$LOG_PREFIX [$logLabel]"

    private lateinit var valueById: MutableMap<ResourceId, V>

    init {
        if (exists()) {
            readFromDisk { valueById = it.toMutableMap() }

            Log.d(label, "loaded ${valueById.size} values")
        } else {
            valueById = mutableMapOf()

            Log.d(label, "created empty storage")
        }
    }

    fun getValue(id: ResourceId) =
        valueById[id] ?: monoid.neutral

    fun setValue(id: ResourceId, value: V) {
        if (value != monoid.neutral) {
            valueById[id] = value
        } else {
            remove(id)
        }
    }

    open fun remove(id: ResourceId) {
        Log.d(label, "forgetting resource $id")
        valueById.remove(id)
    }

    fun refresh() {
        syncWithDisk()
    }

    fun persist() {
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
    private fun syncWithDisk() {
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
                Log.d(label, "resource $addedId appeared from outside")
                setValue(addedId, it[addedId]!!)
            }

            for (knownId in it.keys.intersect(valueById.keys)) {
                val theirs = it[knownId]!!
                val ours = getValue(knownId)
                if (theirs != ours) {
                    setValue(knownId, monoid.combine(ours, theirs))
                }
            }
        }
    }

    /* Reading updated parts of the storage,
     * the timestamp will be updated when `handle` function
     * finished processing new map. */
    protected abstract fun readFromDisk(handle: (Map<ResourceId, V>) -> Unit)

    /* Plain dump of values mapping into filesystem. */
    protected abstract fun writeToDisk(valueById: Map<ResourceId, V>)

    protected abstract fun exists(): Boolean

    protected abstract fun erase()

    protected fun check(value: V): Boolean {
        if (value == monoid.neutral) {
            Log.w(label, "Storage is excessive")
            return true
        }

        return false
    }

    private fun writeToDisk() {
        if (valueById.isEmpty() || valueById.all { it.value == monoid.neutral }) {
            if (exists()) {
                Log.d(label, "no actual data, deleting the file")
                erase()
            }

            return
        }

        writeToDisk(valueById.toMap())
    }
}

private const val LOG_PREFIX: String = "[storage]"