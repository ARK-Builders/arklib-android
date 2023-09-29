package space.taran.arklib

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.user.score.Score

fun randomResourceId(): ResourceId =
    ResourceId(
        (Math.random() * 1000).toLong() + 1,
        (Math.random() * 1000).toLong(),
    )

fun randomScore(max: Int = 1000): Score =
    (Math.random() * (max - 1)).toInt() + 1

fun randomPositiveInt(max: Int = 1000): Int {
    if (max < 10) {
        throw Exception("parameter max value must be greater than equal to 10")
    }
    val value = (Math.random() * max).toInt()
    return if (value > 0) {
        value
    } else {
        randomPositiveInt()
    }
}