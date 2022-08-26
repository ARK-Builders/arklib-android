package space.taran.arklib.index
import java.nio.file.attribute.FileTime

typealias ResourceId = Long

typealias ResourceMetas = Map<String, ResourceMeta>

class RustResourcesIndex(root_path: String, res: ResourceMetas) {
    private val innerPtr: Long
    init {
        innerPtr = init(root_path, res)
    }
    private external fun init(root_path: String, resources: ResourceMetas): Long
    external fun listResources(prefix: String): ArrayList<ResourceMeta>
    external fun getPath(id: ResourceId): String
    // TODO
    external fun getMeta(id: ResourceId): ResourceMeta
    fun test() {

    }
    external fun reindex()
    external fun remove(id: ResourceId)
    external fun updateResource()
}

data class ResourceMeta(
    val id: ResourceId,
    val name: String,
    val extension: String,
    val modified: FileTime,
    val size: Long,
    val kind: ResourceKind?
)

    fun main(){
        val test = ResourceMeta(1,"","",FileTime.fromMillis(111),1,ResourceKind.Video(1,11,1))

        if (test.kind is ResourceKind.Video) {
            println(test.kind.height)
        }
    }

sealed class ResourceKind(val code: KindCode) {
    class Image : ResourceKind(KindCode.IMAGE)

    class Video(
        val height: Long? = null,
        val width: Long? = null,
        val duration: Long? = null
    ) : ResourceKind(KindCode.VIDEO)

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
    IMAGE, VIDEO, DOCUMENT, LINK, PLAINTEXT, ARCHIVE
}

enum class MetaExtraTag {
    DURATION, WIDTH, HEIGHT, PAGES, TITLE, DESCRIPTION, URL
}