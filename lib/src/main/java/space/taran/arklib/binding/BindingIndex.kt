package dev.arkbuilders.arklib.binding

import dev.arkbuilders.arklib.ResourceId
import java.nio.file.Path
import kotlin.io.path.Path

class RawUpdates(
    val deleted: HashSet<ResourceId>,
    val added: HashMap<ResourceId, String>,
)

object BindingIndex {
    private external fun loadNative(root: String): Boolean
    fun load(root: Path): Boolean = loadNative(root.toString())

    private external fun updateAllNative(root: String): RawUpdates
    fun updateAll(root: Path) = updateAllNative(root.toString())

    private external fun updateOneNative(
        root: String,
        path: String,
        oldId: String
    ): RawUpdates

    fun updateOne(root: Path, path: Path, oldId: ResourceId) =
        updateOneNative(root.toString(), path.toString(), oldId.toString())

    private external fun storeNative(root: String)
    fun store(root: Path) = storeNative(root.toString())

    private external fun id2pathNative(root: String): HashMap<ResourceId, String>
    fun id2path(root: Path): Map<ResourceId, Path> =
        id2pathNative(root.toString()).map { (id, pathStr) ->
            id to Path(pathStr)
        }.toMap()
}