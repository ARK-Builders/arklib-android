package dev.arkbuilders.arklib.data.stats

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.metadata.Kind
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.Tags

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
