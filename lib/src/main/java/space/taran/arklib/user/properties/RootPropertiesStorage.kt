package space.taran.arklib.user.properties

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import space.taran.arklib.arkFolder
import space.taran.arklib.arkProperties
import space.taran.arklib.data.storage.FolderStorage
import java.nio.file.Path

class RootPropertiesStorage(
    private val scope: CoroutineScope,
    val root: Path
) : FolderStorage<Properties>(
    "properties", scope, root.arkFolder().arkProperties(), PropertiesMonoid
), PropertiesStorage {
    override suspend fun valueToBinary(value: Properties): ByteArray =
        Json.encodeToString(value).toByteArray(Charsets.UTF_8)

    override suspend fun valueFromBinary(raw: ByteArray): Properties {
        val text = String(raw, Charsets.UTF_8)
        val json = Json.parseToJsonElement(text)

        return Json.decodeFromJsonElement(
            Properties.serializer(),
            json
        )
    }
}