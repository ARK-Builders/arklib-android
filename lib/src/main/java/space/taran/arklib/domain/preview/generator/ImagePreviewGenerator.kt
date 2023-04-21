package space.taran.arklib.domain.preview.generator

import com.bumptech.glide.Glide
import space.taran.arklib.app
import space.taran.arklib.domain.meta.Kind
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.preview.Preview
import space.taran.arklib.domain.preview.PreviewGenerator
import java.nio.file.Path

object ImagePreviewGenerator: PreviewGenerator {

    override fun isValid(path: Path, meta: Metadata): Boolean {
        return meta.kind == Kind.IMAGE
    }

    override suspend fun generate(path: Path, meta: Metadata): Result<Preview> {
        val bitmap = Preview.downscale(
            Glide.with(app).asBitmap().load(path.toFile())
        )

        return Result.success(Preview(bitmap, onlyThumbnail = true))
    }
}
