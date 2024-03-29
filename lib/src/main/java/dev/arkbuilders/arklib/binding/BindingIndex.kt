package dev.arkbuilders.arklib.binding

import dev.arkbuilders.arklib.ResourceId
import java.nio.file.Path
import kotlin.io.path.Path

class RawUpdates(
    val deleted: Set<ResourceId>,
    val added: Map<ResourceId, Path>
)

object BindingIndex {
    private external fun loadNative(root: String): Boolean
    fun load(root: Path): Boolean = loadNative(root.toString())

    private external fun updateNative(root: String): List<Any>
    fun update(root: Path): RawUpdates {
        val list = updateNative(root.toString())
        val deleted = (list[0] as List<String>).map {
            ResourceId.fromString(it)
        }.toSet()
        val added = (list[1] as HashMap<String, String>).map { (id, path) ->
            ResourceId.fromString(id) to Path(path)
        }.toMap()
        return RawUpdates(deleted, added)
    }

    private external fun storeNative(root: String)
    fun store(root: Path) = storeNative(root.toString())

    private external fun id2pathNative(root: String): HashMap<String, String>
    fun id2path(root: Path): Map<ResourceId, Path> =
        id2pathNative(root.toString()).map { (idStr, pathStr) ->
            ResourceId.fromString(idStr) to Path(pathStr)
        }.toMap()
}