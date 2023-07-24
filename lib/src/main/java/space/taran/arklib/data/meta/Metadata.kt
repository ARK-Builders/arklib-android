package space.taran.arklib.data.meta

import kotlinx.serialization.Serializable
import space.taran.arklib.data.processor.AggregateProcessor
import space.taran.arklib.data.processor.Processor

enum class Kind {
    IMAGE, VIDEO, DOCUMENT, LINK, PLAINTEXT, ARCHIVE, UNKNOWN
}

// used for JSON parsing, must be the same as name
// of the `Metadata` constructor field
const val KIND: String = "kind"

@Serializable
sealed class Metadata(val kind: Kind) {
    @Serializable
    class Image: Metadata(Kind.IMAGE)

    @Serializable
    class Video(
        val height: Long? = null,
        val width: Long? = null,
        val duration: Long? = null // milliseconds
    ): Metadata(Kind.VIDEO)

    @Serializable
    class Document(
        val isPdf: Boolean = false,
        val pages: Int? = null
    ): Metadata(Kind.DOCUMENT)

    @Serializable
    class Link(
        val title: String? = null,
        val description: String? = null
    ): Metadata(Kind.LINK)

    @Serializable
    class PlainText: Metadata(Kind.PLAINTEXT)

    @Serializable
    class Archive: Metadata(Kind.ARCHIVE)

    @Serializable
    class Unknown: Metadata(Kind.UNKNOWN)
}

typealias MetadataProcessor = Processor<Metadata, MetadataUpdate>

typealias AggregateMetadataProcessor = AggregateProcessor<Metadata, MetadataUpdate>

internal const val LOG_PREFIX: String = "[metadata]"