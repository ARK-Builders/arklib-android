package space.taran.arknavigator.mvp.model.repo.index

import java.nio.file.Path

typealias ResourceId = Long

private external fun computeIdNative(size: Long, file: String): Long

fun computeId(size: Long, file: Path): ResourceId {
    return computeIdNative(size, file.toString())
}