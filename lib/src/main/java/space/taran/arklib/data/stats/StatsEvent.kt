package space.taran.arklib.data.stats

import space.taran.arklib.ResourceId
import space.taran.arklib.data.meta.Kind
import space.taran.arklib.user.tags.Tag
import space.taran.arklib.user.tags.Tags

sealed class StatsEvent {
    data class TagsChanged(
        val resource: ResourceId,
        val oldTags: Tags,
        val newTags: Tags
    ) : StatsEvent()

    data class PlainTagUsed(val tag: Tag) : StatsEvent()

    data class KindTagUsed(
        val kind: Kind
    ) : StatsEvent()
}
