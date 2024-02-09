package dev.arkbuilders.arklib.data.storage

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.utils.deleteRecursively
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo

abstract class FolderStorage<V>(
    private val label: String,
    private val scope: CoroutineScope,
    private val storageFolder: Path,
    monoid: Monoid<V>,
) : BaseStorage<V>(label, scope, monoid) {

    /* Folder storage is more flexible, allowing to store
     * arbitrary type of data as values. It could be images as well.
     * Inheritants must define binary conversions for their type. */
    protected abstract suspend fun valueToBinary(value: V): ByteArray
    protected abstract suspend fun valueFromBinary(raw: ByteArray): V

    private var diskTimestamps: ConcurrentHashMap<ResourceId, FileTime> =
        ConcurrentHashMap()

    // Can be changed by setValue or when writing to disk
    private var ramTimestamps: ConcurrentHashMap<ResourceId, FileTime> =
        ConcurrentHashMap()

    override suspend fun afterInit() {
        ramTimestamps.putAll(diskTimestamps)
    }

    final override fun erase() {
        scope.launch(Dispatchers.IO) {
            deleteRecursively(storageFolder)
        }
    }

    final override fun exists(): Boolean {
        val result = Files.exists(storageFolder)

        val not = if (result) "" else " not"
        Log.d(logPrefix, "folder $storageFolder does$not exist")
        return result
    }

    override fun setValue(id: ResourceId, value: V) {
        ramTimestamps[id] = FileTime.from(Instant.now())
        super.setValue(id, value)
    }

    override fun remove(id: ResourceId) {
        super.remove(id)

        if (diskTimestamps[id]!! >= Files.getLastModifiedTime(pathFromId(id))) {
            scope.launch(Dispatchers.IO) {
                pathFromId(id).deleteIfExists()
            }
            ramTimestamps.remove(id)
            diskTimestamps.remove(id)
        }
    }

    // passes only new values into the `handle` callback
    final override suspend fun readFromDisk(handle: (Map<ResourceId, V>) -> Unit) {
        val newValueById: ConcurrentHashMap<ResourceId, V> = ConcurrentHashMap()
        val newTimestamps: ConcurrentHashMap<ResourceId, FileTime> =
            ConcurrentHashMap()

        val stream = Files.list(storageFolder)
            .filter { !it.isDirectory() }
            .map { path ->
                Log.v(logPrefix, "reading value from $path")
                val id = idFromPath(path)

                scope.launch(Dispatchers.IO) {
                    val timestamp = diskTimestamps[id]
                    val newTimestamp = Files.getLastModifiedTime(path)

                    if (timestamp == null || timestamp < newTimestamp) {
                        val binary = Files.readAllBytes(path)

                        val value = valueFromBinary(binary)

                        if (isNeutral(value)) {
                            throw StorageException(label, "Empty value can be indicator of dirty write")
                        }

                        newValueById[id] = value
                        newTimestamps[id] = newTimestamp
                    }
                }
            }

        val jobs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            stream.toList()
        } else {
            stream.collect(Collectors.toList())
        }

        jobs.joinAll()

        Log.d(logPrefix, "${newValueById.size} entries has been read")

        handle(newValueById)

        // during merge, the file could be modified again
        // it will be handled during next sync
        diskTimestamps.putAll(newTimestamps)
    }

    override suspend fun writeToDisk(valueById: Map<ResourceId, V>) {
        Files.createDirectories(storageFolder)

        val changedValueByIds = findChangedIds().map { id ->
            id to valueById[id]!!
        }.toMap()

        changedValueByIds.forEach {
            if (isNeutral(it.value)) {
                throw IllegalStateException("Storage is excessive")
            }

            val timestamp = diskTimestamps[it.key]

            val file = pathFromId(it.key)
            Files.write(file, valueToBinary(it.value))

            val newTimestamp = Files.getLastModifiedTime(file)
            if (newTimestamp == timestamp) {
                throw IllegalStateException("Timestamp didn't update")
            }

            diskTimestamps[it.key] = newTimestamp
            ramTimestamps[it.key] = newTimestamp
        }

        Log.d(logPrefix, "${changedValueByIds.size} entries have been written")
    }


    private fun findChangedIds() = ramTimestamps.mapNotNull { (id, ramFt) ->
        // If id is in RAM, but not on disk, then we must write it
        val diskFt = diskTimestamps[id] ?: return@mapNotNull id

        if (diskFt != ramFt) {
            return@mapNotNull id
        }

        return@mapNotNull null
    }


    private fun pathFromId(id: ResourceId): Path =
        storageFolder.resolve(id.toString())

    private fun idFromPath(path: Path): ResourceId =
        // checking that we are indeed in correct storage
        ResourceId.fromString(
            path.relativeTo(storageFolder)
                .fileName.toString()
        )

    private val logPrefix = "$LOG_PREFIX [$label]"
}

private const val LOG_PREFIX: String = "[folder-storage]"
