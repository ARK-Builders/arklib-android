package space.taran.arklib.domain.storage

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.taran.arklib.ResourceId
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.attribute.FileTime

abstract class FileStorage<V>(
    private val label: String,
    private val scope: CoroutineScope,
    private val storageFile: Path,
    monoid: Monoid<V>
) : BaseStorage<V>(label, scope, monoid) {

    /* The file will be filled with a table,
     * one mapping entry per line of the table.
     * We only need to read and write values into strings for this. */
    protected abstract fun valueToString(value: V): String
    protected abstract fun valueFromString(raw: String): V

    private var timestamp: FileTime = FileTime.fromMillis(0L)

    final override fun erase() {
        scope.launch(Dispatchers.IO) {
            Files.delete(storageFile)
        }
    }

    final override fun exists(): Boolean {
        val result = Files.exists(storageFile)

        val not = if (result) "" else " not"
        Log.d(logPrefix, "folder $storageFile does$not exist")
        return result
    }

    // returns all values from new file,
    // we don't have more granular timestamping
    override suspend fun readFromDisk(handle: (Map<ResourceId, V>) -> Unit) {
        val newTimestamp = Files.getLastModifiedTime(storageFile)
        Log.d(logPrefix, "timestamp of storage file $storageFile is $timestamp")

        if (timestamp >= newTimestamp) {
            return
        }
        Log.d(logPrefix, "the file was modified externally, merging")

        val lines = Files.readAllLines(storageFile, StandardCharsets.UTF_8)

        verifyVersion(label, lines.removeAt(0))

        val valueById = try {
            lines.associate {
                val parts = it.split(KEY_VALUE_SEPARATOR)
                val id = ResourceId.fromString(parts[0])
                val value = valueFromString(parts[1])

                if (isNeutral(value)) {
                    throw StorageException(
                        label,
                        "Empty value can be indicator of dirty write"
                    )
                }

                id to value
            }
        } catch (e: Throwable) {
            if (e is StorageException) {
                throw e
            } else {
                Log.w(
                    label,
                    "Error while parsing lines, stack trace:"
                )
                e.printStackTrace()
                throw StorageException(
                    label,
                    "Error while parsing lines, looks like a dirty write",
                    error = e
                )
            }
        }

        if (valueById.isEmpty()) {
            throw StorageException(
                label,
                "Empty storage can be indicator of dirty write"
            )
        }

        Log.d(logPrefix, "${valueById.size} entries have been read")

        handle(valueById)
        timestamp = newTimestamp
    }

    override suspend fun writeToDisk(valueById: Map<ResourceId, V>) {
        val lines = mutableListOf<String>()
        lines.add("$STORAGE_VERSION_PREFIX$STORAGE_VERSION")

        lines.addAll(
            valueById.map { (id, value: V) ->
                if (isNeutral(value)) {
                    throw IllegalStateException("Storage is excessive")
                }
                "$id$KEY_VALUE_SEPARATOR${valueToString(value)}"
            }
        )

        Files.createDirectories(storageFile.parent)
        Files.write(storageFile, lines, StandardCharsets.UTF_8)

        val newTimestamp = Files.getLastModifiedTime(storageFile)
        if (newTimestamp == timestamp) {
            throw IllegalStateException("Timestamp didn't update")
        }
        timestamp = newTimestamp

        Log.d(logPrefix, "${valueById.size} entries has been written")
    }

    companion object {
        private const val STORAGE_VERSION = 2
        private const val STORAGE_VERSION_PREFIX = "version "

        const val KEY_VALUE_SEPARATOR = ':'

        private fun verifyVersion(label: String, header: String) {
            if (!header.startsWith(STORAGE_VERSION_PREFIX)) {
                throw StorageException(label, "Unknown storage version")
            }
            val version = header.removePrefix(STORAGE_VERSION_PREFIX).toInt()

            if (version > STORAGE_VERSION) {
                throw StorageException(label, "Storage format is newer than the app")
            }
            if (version < STORAGE_VERSION) {
                throw StorageException(label, "Storage format is older than the app")
            }
        }
    }

    private val logPrefix = "$LOG_PREFIX [$label]"
}

private const val LOG_PREFIX: String = "[file-storage]"