package dev.arkbuilders.arklib.user.tags

import android.util.Log
import dev.arkbuilders.arklib.data.storage.Monoid

typealias Tag = String

typealias Tags = Set<Tag>

object TagUtils {
    fun validateTag(tag: Tag): Tag? {
        val validated = tag.trim()
        if (validated.isEmpty()) return null
        return validated
    }   
}

object TagsMonoid: Monoid<Tags> {
    override val neutral: Tags = emptySet()

    override fun combine(a: Tags, b: Tags): Tags {
        val result = a.union(b)

        Log.d(LOG_PREFIX, "merging $a and $b into $result")
        return result
    }
}

internal val LOG_PREFIX: String = "[tags]"
