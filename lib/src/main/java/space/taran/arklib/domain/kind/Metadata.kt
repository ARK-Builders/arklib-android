package space.taran.arklib.domain.kind

import kotlinx.serialization.Serializable

@Serializable
sealed class Metadata(val code: KindCode) {
    @Serializable
    class Image : Metadata(KindCode.IMAGE)

    @Serializable
    class Video(
        val height: Long? = null,
        val width: Long? = null,
        val duration: Long? = null
    ) : Metadata(KindCode.VIDEO)

    @Serializable
    class Document(val pages: Int? = null) : Metadata(KindCode.DOCUMENT)

    @Serializable
    class Link(
        val title: String? = null,
        val description: String? = null
    ) : Metadata(KindCode.LINK)

    @Serializable
    class PlainText : Metadata(KindCode.PLAINTEXT)

    @Serializable
    class Archive : Metadata(KindCode.ARCHIVE)
}

enum class KindCode {
    IMAGE, VIDEO, DOCUMENT, LINK, PLAINTEXT, ARCHIVE
}
