package space.taran.arklib.user.tags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkTags
import space.taran.arklib.data.stats.StatsEvent
import space.taran.arklib.data.storage.FileStorage
import java.nio.file.Path

class RootTagsStorage(
    private val scope: CoroutineScope,
    val root: Path,
    private val statsFlow: MutableSharedFlow<StatsEvent>,
) : FileStorage<Tags>(
    "tags", scope, root.arkFolder().arkTags(), TagsMonoid
), TagStorage {

    override fun valueToString(value: Tags): String =
        value.joinToString(",")

    override fun valueFromString(raw: String): Tags =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    override fun remove(id: ResourceId) {
        scope.launch {
            statsFlow.emit(
                StatsEvent.TagsChanged(
                    id,
                    oldTags = getValue(id),
                    newTags = emptySet()
                )
            )
        }
        super.remove(id)
    }

    override fun setTags(id: ResourceId, tags: Tags) {
        scope.launch {
            statsFlow.emit(
                StatsEvent.TagsChanged(
                    id,
                    oldTags = getValue(id),
                    newTags = tags
                )
            )
        }
        super.setTags(id, tags)
    }
}