package dev.arkbuilders.arklib.data.preview.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import dev.arkbuilders.arklib.app
import dev.arkbuilders.arklib.data.metadata.Kind
import dev.arkbuilders.arklib.data.metadata.Metadata
import dev.arkbuilders.arklib.data.preview.Preview
import dev.arkbuilders.arklib.data.preview.PreviewGenerator
import java.nio.file.Path
import kotlin.io.path.readLines

object TxtPreviewGenerator : PreviewGenerator {

    override fun isValid(path: Path, meta: Metadata): Boolean {
        return meta.kind == Kind.PLAINTEXT
    }

    override suspend fun generate(path: Path, meta: Metadata): Result<Preview> =
        generateBitmap(path).map {
            Preview(it, onlyThumbnail = true)
        }

    // it is padding in preview image
    private val padding = 2f * app.resources.displayMetrics.density

    private fun generateBitmap(path: Path): Result<Bitmap> {
        val lines = path.readLines()
        val text = lines.take(10).joinToString("\n")

        val textPaint = TextPaint()
        textPaint.isAntiAlias = true
        textPaint.textSize = 5f
        textPaint.color = -0x1000000

        val bitmap = Bitmap.createBitmap(
            Preview.THUMBNAIL_SIZE,
            Preview.THUMBNAIL_SIZE,
            Bitmap.Config.ARGB_8888
        )

        val staticLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            // right is padding in preview image
            Preview.THUMBNAIL_SIZE - (padding.toInt())
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(5f, 0.9f)
            .setIncludePad(true)
            .build()

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawPaint(paint)
        canvas.save()
        canvas.translate(padding, padding) // Left top padding in preview image.
        staticLayout.draw(canvas)
        canvas.restore()

        return Result.success(bitmap)
    }
}
