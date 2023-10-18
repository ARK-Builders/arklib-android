package dev.arkbuilders.arklib.data.preview.generator

import dev.arkbuilders.arklib.data.meta.Kind
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.preview.Preview
import dev.arkbuilders.arklib.data.preview.PreviewGenerator
import java.nio.file.Path

object ImagePreviewGenerator: PreviewGenerator {

    override fun isValid(path: Path, meta: Metadata): Boolean {
        return meta.kind == Kind.IMAGE
    }

    override suspend fun generate(path: Path, meta: Metadata): Result<Preview> {
        val bitmap = Preview.downscale(
            path,
            path.toFile()
        )

        return Result.success(Preview(bitmap, onlyThumbnail = true))
    }
}
