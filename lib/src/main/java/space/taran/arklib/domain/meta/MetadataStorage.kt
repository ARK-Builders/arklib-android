package space.taran.arklib.domain.meta

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import space.taran.arklib.domain.storage.FolderStorage
import space.taran.arklib.domain.storage.MonoidIsNotUsed
import java.nio.file.Path

internal class MetadataStorage(
    scope: CoroutineScope, path: Path
) : FolderStorage<Metadata>(
    "metadata", scope, path, MonoidIsNotUsed()
) {
    override fun isNeutral(value: Metadata): Boolean = false

    override suspend fun valueToBinary(value: Metadata): ByteArray {
        val json = when (value) {
            is Metadata.Archive -> Json.encodeToString(value)
            is Metadata.Document -> Json.encodeToString(value)
            is Metadata.Image -> Json.encodeToString(value)
            is Metadata.Link -> Json.encodeToString(value)
            is Metadata.PlainText -> Json.encodeToString(value)
            is Metadata.Video -> Json.encodeToString(value)
            is Metadata.Unknown -> Json.encodeToString(value)
        }

        return json.toByteArray(Charsets.UTF_8)
    }

    override suspend fun valueFromBinary(raw: ByteArray): Metadata {
        val text = String(raw, Charsets.UTF_8)
        val json = Json.parseToJsonElement(text)

        val kind = json.jsonObject[KIND]!!.jsonPrimitive.content

        val metadata = when (Kind.valueOf(kind)) {
            Kind.IMAGE ->
                Json.decodeFromJsonElement(
                    Metadata.Image.serializer(),
                    json
                )
            Kind.VIDEO ->
                Json.decodeFromJsonElement(
                    Metadata.Video.serializer(),
                    json
                )
            Kind.DOCUMENT ->
                Json.decodeFromJsonElement(
                    Metadata.Document.serializer(),
                    json
                )
            Kind.LINK -> Json.decodeFromJsonElement(
                Metadata.Link.serializer(),
                json
            )
            Kind.PLAINTEXT -> Json.decodeFromJsonElement(
                Metadata.PlainText.serializer(),
                json
            )
            Kind.ARCHIVE -> Json.decodeFromJsonElement(
                Metadata.Archive.serializer(),
                json
            )
            Kind.UNKNOWN -> Json.decodeFromJsonElement(
                Metadata.Unknown.serializer(),
                json
            )
        }

        return metadata
    }
}