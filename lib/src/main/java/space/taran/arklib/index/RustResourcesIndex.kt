package space.taran.arklib.index

import java.nio.file.Path
import java.nio.file.attribute.FileTime

typealias ResourceId = Long

typealias ResourceMetas = MutableMap<Path, ResourceMeta>

class RustResourcesIndex(root_path: String, res: ResourceMetas) {
    private val innerPtr: Long
    init {
        innerPtr = init(root_path, res)
    }
    private external fun init(root_path: String, resources: MutableMap<Path,ResourceMeta>): Long
    external fun listResources(prefix: String): ResourceMetas
    external fun getPath(id: ResourceId): Path
    external fun listIds(): ArrayList<Long>
    external fun getMeta(id: ResourceId): ResourceMeta
    // TODO
    external fun reindex(): Difference
    external fun remove(id: ResourceId): Path
    external fun updateResource(path: Path, newResource: ResourceMeta)
}

data class Difference(val deleted: ResourceMetas, val added: ResourceMetas)

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

    class Document(val pages: Long? = null) : ResourceKind(KindCode.DOCUMENT)

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
