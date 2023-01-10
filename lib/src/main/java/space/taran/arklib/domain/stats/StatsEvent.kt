package space.taran.arklib.domain.stats

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.kind.KindCode
import space.taran.arklib.domain.tags.Tag
import space.taran.arklib.domain.tags.Tags

sealed class StatsEvent {
    data class TagsChanged(
        val resource: ResourceId,
        val oldTags: Tags,
        val newTags: Tags
    ) : StatsEvent()

    data class PlainTagUsed(val tag: Tag) : StatsEvent()

    data class KindTagUsed(
        val kindCode: KindCode
    ) : StatsEvent()
}