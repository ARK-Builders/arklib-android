package dev.arkbuilders.arklib.data.metadata.extractor

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.metadata.Metadata
import dev.arkbuilders.arklib.data.metadata.MetadataExtractor
import dev.arkbuilders.arklib.utils.detectMimeType
import dev.arkbuilders.arklib.utils.extension
import java.io.FileNotFoundException
import java.nio.file.Path

object DocumentMetadataExtractor: MetadataExtractor {

    override val acceptedExtensions: Set<String>
        get() = setOf("csv", "pdf", "odt", "odp", "ods",
            "rtf", "ppt", "pptx", "xls", "xlsx",
            "doc", "docx", "markdown", "md")

    override val acceptedMimeTypes: Set<String>
        get() = setOf(
            "text/csv",
            "application/pdf",
            "application/vnd.oasis.opendocument.presentation",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.text",
            "application/rtf",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/markdown"
        )

    override fun extract(path: Path, resource: Resource): Result<Metadata> {
        if (!isPdf(path)) {
            return Result.success(
                Metadata.Document(isPdf = false, pages = null)
            )
        }

        var parcelFileDescriptor: ParcelFileDescriptor? = null;
        return try {
            parcelFileDescriptor = ParcelFileDescriptor.open(
                path.toFile(),
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val totalPages = pdfRenderer.pageCount
            val pages = if (totalPages > 0) totalPages else null

            Result.success(
                Metadata.Document(isPdf = true, pages = pages)
            )
        } catch (e: FileNotFoundException) {
            Result.failure(FileNotFoundException(path.toString()))
        } finally {
            parcelFileDescriptor?.close()
        }
    }

    private const val PDF_EXTENSION: String = "pdf"
    private const val PDF_MIME_TYPE: String = "application/pdf"

    private fun isPdf(path: Path): Boolean =
        extension(path) == PDF_EXTENSION || detectMimeType(path) == PDF_MIME_TYPE
}
