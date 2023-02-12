package space.taran.arklib.domain.kind

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ResourceKind(val code: KindCode) {
    @Serializable
    class Image : ResourceKind(KindCode.IMAGE)

    @Serializable
    class Video(
        val height: Long? = null,
        val width: Long? = null,
        val duration: Long? = null
    ) : ResourceKind(KindCode.VIDEO)

    @Serializable
    class Document(val pages: Int? = null) : ResourceKind(KindCode.DOCUMENT)

    @Serializable
    class Link(
        val title: String? = null,
        val description: String? = null,
        val url: String? = null
    ) : ResourceKind(KindCode.LINK)

    @Serializable
    class PlainText : ResourceKind(KindCode.PLAINTEXT)

    @Serializable
    class Archive : ResourceKind(KindCode.ARCHIVE)
}

enum class KindCode {
    IMAGE, VIDEO, DOCUMENT, LINK, PLAINTEXT, ARCHIVE
}
