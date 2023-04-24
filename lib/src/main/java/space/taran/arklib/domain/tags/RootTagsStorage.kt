package space.taran.arklib.domain.tags

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkTags
import space.taran.arklib.domain.storage.FileStorage
import java.nio.file.Path

class RootTagsStorage(
    scope: CoroutineScope,
    val root: Path): FileStorage<Tags>(
        scope, root.arkFolder().arkTags(), TagsMonoid, "tags") {

    override fun valueToString(value: Tags): String =
        value.joinToString(",")
    override fun valueFromString(raw: String): Tags =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    fun getTags(id: ResourceId): Tags =
        getValue(id) as Tags

    fun getTags(ids: Iterable<ResourceId>): Tags =
        ids.flatMap { id -> getTags(id) }.toSet()

    fun groupTagsByResources(ids: Iterable<ResourceId>): Map<ResourceId, Tags> =
        ids.map { it to getTags(it) }.toMap()

    fun setTags(id: ResourceId, tags: Tags) =
        setValue(id, tags)
}