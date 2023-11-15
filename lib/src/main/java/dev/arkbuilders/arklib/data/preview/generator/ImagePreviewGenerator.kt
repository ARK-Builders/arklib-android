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
        val thumbnailResult = Preview.downscale(
            path,
            path.toFile()
        )

        return thumbnailResult.map { thumbnail ->
            Preview(thumbnail, onlyThumbnail = true)
        }
    }
}
