package space.taran.arklib.index

import java.nio.file.Path
import java.nio.file.attribute.FileTime
typealias ResourceId = Long

typealias RawIndex = MutableMap<Path, ResourceMeta>

class RustResourcesIndex(root_path: String, res: RawIndex) {
    private val innerPtr: Long
    init {
        innerPtr = init(root_path, res)
    }
    private external fun init(root_path: String, resources: MutableMap<Path, ResourceMeta>): Long
    external fun listResources(prefix: String?): RawIndex
    external fun getPath(id: ResourceId): Path?
    // TODO
    external fun getMeta(id: ResourceId): ResourceMeta?
    external fun contains(id: ResourceId): Boolean
    external fun reindex(): Difference
    external fun remove(id: ResourceId): Path
    external fun updateResource(path: Path, newResource: ResourceMeta)
}

data class Difference(val deleted: RawIndex, val added: RawIndex)

data class ResourceMeta(
        val id: Long,
        val name: String,
        val extension: String,
        val modified: FileTime,
        val size: Long,
        val kind: ResourceKind?
)

sealed class ResourceKind(val code: KindCode) {
    class Image : ResourceKind(KindCode.IMAGE)

    class Video(val height: Long? = null, val width: Long? = null, val duration: Long? = null) :
            ResourceKind(KindCode.VIDEO)

    class Document(val pages: Int? = null) : ResourceKind(KindCode.DOCUMENT)

    class Link(
            val title: String? = null,
            val description: String? = null,
            val url: String? = null
    ) : ResourceKind(KindCode.LINK)

    class PlainText : ResourceKind(KindCode.PLAINTEXT)

    class Archive : ResourceKind(KindCode.ARCHIVE)
}

// These enums are only used to store different kind in one table in Room
enum class KindCode {
    IMAGE,
    VIDEO,
    DOCUMENT,
    LINK,
    PLAINTEXT,
    ARCHIVE
}

enum class MetaExtraTag {
    DURATION,
    WIDTH,
    HEIGHT,
    PAGES,
    TITLE,
    DESCRIPTION,
    URL
}