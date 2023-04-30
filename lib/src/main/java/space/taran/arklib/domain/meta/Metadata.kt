package space.taran.arklib.domain.meta

import kotlinx.serialization.Serializable
import space.taran.arklib.domain.processor.AggregateProcessor
import space.taran.arklib.domain.processor.Processor

enum class Kind {
    IMAGE, VIDEO, DOCUMENT, LINK, PLAINTEXT, ARCHIVE
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
}

typealias MetadataProcessor = Processor<Metadata, MetadataUpdate>

typealias AggregateMetadataProcessor = AggregateProcessor<Metadata, MetadataUpdate>

internal const val LOG_PREFIX: String = "[metadata]"