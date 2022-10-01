package space.taran.arknavigator.mvp.model.repo.index

import java.nio.file.Path

typealias ResourceId = Long

fun computeId(size: Long, file: Path): ResourceId {
    return space.taran.arklib.computeId(size, file)
}