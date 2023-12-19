package dev.arkbuilders.arklib.user.properties

import android.util.Log
import kotlinx.serialization.Serializable
import dev.arkbuilders.arklib.data.storage.Monoid

@Serializable
class Properties(
    val titles: Set<String>,
    val descriptions: Set<String>
)

object PropertiesMonoid: Monoid<Properties> {
    override val neutral: Properties = Properties(
        emptySet(),
        emptySet()
    )

    override fun combine(a: Properties, b: Properties): Properties {
        val result = Properties(
            a.titles.union(b.titles),
            a.descriptions.union(b.descriptions),
        )

        Log.v(LOG_PREFIX, "merging $a and $b into $result")
        return result
    }
}

internal val LOG_PREFIX: String = "[properties]"