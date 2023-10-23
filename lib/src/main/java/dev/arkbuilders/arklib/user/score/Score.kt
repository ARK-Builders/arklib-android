package dev.arkbuilders.arklib.user.score

import android.util.Log
import dev.arkbuilders.arklib.data.storage.Monoid
import java.lang.Math.abs

typealias Score = Int

object ScoreMonoid: Monoid<Score> {
    override val neutral: Score = 0

    override fun combine(a: Score, b: Score): Score {
        val result = if (abs(a) > abs(b)) { a } else { b }

        Log.v(LOG_PREFIX, "merging $a and $b into $result")
        return result
    }
}

internal val LOG_PREFIX: String = "[score]"
