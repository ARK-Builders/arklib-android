package space.taran.arklib.domain.storage

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.taran.arklib.ResourceId
import space.taran.arklib.utils.deleteRecursively
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo

abstract class FolderStorage<V>(
    private val scope: CoroutineScope,
    private val storageFolder: Path,
    monoid: Monoid<V>,
    logLabel: String) : BaseStorage<V>(monoid, logLabel) {

    /* Folder storage is more flexible, allowing to store
     * arbitrary type of data as values. It could be images as well.
     * Inheritants must define binary conversions for their type. */
    protected abstract fun valueToBinary(value: V): ByteArray
    protected abstract fun valueFromBinary(raw: ByteArray): V

    private var timestamps: MutableMap<ResourceId, FileTime> = mutableMapOf()

    final override fun erase() {
        scope.launch(Dispatchers.IO) {
            deleteRecursively(storageFolder)
        }
    }

    final override fun exists(): Boolean {
        val result = Files.exists(storageFolder)

        val not = if (result) "" else " not"
        Log.d(label, "folder $storageFolder does$not exist")
        return result
    }

    override fun remove(id: ResourceId) {
        super.remove(id)

        if (timestamps[id]!! >= Files.getLastModifiedTime(pathFromId(id))) {
            scope.launch(Dispatchers.IO) {
                pathFromId(id).deleteIfExists()
            }
            timestamps.remove(id)
        }
    }

    // returns only new values
    final override fun readFromDisk(handle: (Map<ResourceId, V>) -> Unit) {
        val newValueById: MutableMap<ResourceId, V> = mutableMapOf()
        val newTimestamps: MutableMap<ResourceId, FileTime> = mutableMapOf()

        Files.list(storageFolder)
            .filter { !it.isDirectory() }
            .forEach { path ->
                Log.d(label, "reading value from $path")
                val id = idFromPath(path)

                val timestamp = timestamps[id]
                val newTimestamp = Files.getLastModifiedTime(path)

                if (timestamp == null || timestamp < newTimestamp) {
                    val binary = Files.readAllBytes(path)
                    val value = valueFromBinary(binary)

                    check(value)

                    newValueById[id] = value
                    newTimestamps[id] = newTimestamp
                }
            }

        Log.d(label, "${newValueById.size} entries has been read")

        handle(newValueById)

        // during merge, the file could be modified again
        // it will be handled during next sync
        timestamps = newTimestamps
    }

    override fun writeToDisk(valueById: Map<ResourceId, V>) {
        valueById.forEach {
            if (check(it.value)) {
                throw IllegalStateException("Storage is excessive")
            }

            val timestamp = timestamps[it.key]

            val file = pathFromId(it.key)
            Files.write(file, valueToBinary(it.value))

            val newTimestamp = Files.getLastModifiedTime(file)
            if (newTimestamp == timestamp) {
                throw IllegalStateException("Timestamp didn't update")
            }

            timestamps[it.key] = Files.getLastModifiedTime(file)
        }

        Log.d(label, "${valueById.size} entries have been written")
    }

    private fun pathFromId(id: ResourceId): Path =
        storageFolder.resolve(id.toString())

    private fun idFromPath(path: Path): ResourceId =
        // checking that we are indeed in correct storage
        ResourceId.fromString(
            path.relativeTo(storageFolder)
                .fileName.toString()
        )

    private val label = "$LOG_PREFIX [$logLabel]"
}

private const val LOG_PREFIX: String = "[folder-storage]"