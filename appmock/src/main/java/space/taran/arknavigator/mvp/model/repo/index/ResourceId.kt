package space.taran.arknavigator.mvp.model.repo.index

import space.taran.arklib.ResourceId
import space.taran.arklib.computeId
import java.nio.file.Path

fun computeID(size: Long, file: Path): ResourceId {
    return computeId(size, file)
}