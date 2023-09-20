package space.taran.arklib

import dev.arkbuilders.arklib.ResourceId

fun randomResourceId():ResourceId =
    ResourceId(
        (Math.random() * 1000).toLong() + 1,
        (Math.random() * 1000).toLong(),
        )