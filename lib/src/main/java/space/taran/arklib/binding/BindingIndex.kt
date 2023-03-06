package space.taran.arklib.binding

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.UpdatedResources
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.Path

object BindingIndex {
    private external fun loadNative(root: String): Boolean
    fun load(root: Path): Boolean = loadNative(root.toString())

    private external fun buildNative(root: String)
    fun build(root: Path) = buildNative(root.toString())

    private external fun updateNative(root: String): List<Any>
    fun update(root: Path): UpdatedResources {
        val list = updateNative(root.toString())
        val deleted = (list[0] as List<String>).map {
            ResourceId.fromString(it)
        }.toSet()
        val added = (list[1] as HashMap<String, String>).map { (id, path) ->
            ResourceId.fromString(id) to Path(path)
        }.toMap()
        return UpdatedResources(deleted, added)
    }

    private external fun storeNative(root: String)
    fun store(root: Path) = storeNative(root.toString())

    private external fun id2PathNative(root: String): HashMap<String, String>
    fun id2Path(root: Path): Map<ResourceId, Path> =
        id2PathNative(root.toString()).map { (idStr, pathStr) ->
            ResourceId.fromString(idStr) to Path(pathStr)
        }.toMap()

    private external fun path2idNative(root: String): HashMap<String, String>
    fun path2id(root: Path): Map<Path, Pair<ResourceId, FileTime>> =
        path2idNative(root.toString()).map { (pathStr, idToMillis) ->
            val splitted = idToMillis.split(":")
            val id = ResourceId.fromString(splitted[0])
            val fileTime = FileTime.fromMillis(splitted[1].toLong())
            Path(pathStr) to Pair(id, fileTime)
        }.toMap()
}