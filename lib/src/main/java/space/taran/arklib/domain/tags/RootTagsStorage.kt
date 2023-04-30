package space.taran.arklib.domain.tags

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.arkFolder
import space.taran.arklib.arkTags
import space.taran.arklib.domain.storage.FileStorage
import java.nio.file.Path

class RootTagsStorage(
    scope: CoroutineScope,
    val root: Path
) : FileStorage<Tags>(
    scope, root.arkFolder().arkTags(), TagsMonoid, "tags"
), TagStorage {

    override fun valueToString(value: Tags): String =
        value.joinToString(",")
    override fun valueFromString(raw: String): Tags =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
}