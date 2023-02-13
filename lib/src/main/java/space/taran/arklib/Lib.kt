package space.taran.arklib

import android.app.Application
import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.nio.file.Path

internal lateinit var app: Application

data class LinkData(
    val title: String,
    val desc: String,
    val url: String,
    val imageUrl: String
) {
    companion object {
        @JvmStatic
        fun create(
            title: String,
            desc: String,
            url: String,
            imageUrl: String
        ): LinkData = LinkData(title, desc, url, imageUrl)
    }
}

@Parcelize
data class ResourceId(
    val dataSize: Long,
    val crc32: Long
) : Parcelable {

    override fun toString() =
        dataSize.toString()
            .plus(KEY_VALUE_SEPARATOR)
            .plus(crc32)

    companion object {

        const val KEY_VALUE_SEPARATOR = "-"

        fun fromString(str: String): ResourceId {
            val parts = str.split(KEY_VALUE_SEPARATOR)
                return ResourceId(
                    parts[0].toLong(),
                    parts[1].toLong()
           )
        }

        @JvmStatic
        fun create(dataSize: Long, crc32: Long): ResourceId =
            ResourceId(dataSize, crc32)
    }
}

private external fun computeIdNative(size: Long, file: String): ResourceId
private external fun createLinkFileNative(
    root: String,
    title: String,
    desc: String,
    url: String,
    basePath: String,
    downloadPreview: Boolean
)

private external fun loadLinkFileNative(root: String, file: String): LinkData
private external fun getLinkHashNative(url: String): String
private external fun fetchLinkDataNative(url: String): LinkData?
private external fun pdfPreviewGenerateNative(path: String, quality: String): Bitmap

/*
 *  Just to allow for preliminary testing with Navigator
 *  A better way may be proposed to get application instance
 *  Navigator should call this function in [Application.onCreate].
 */
fun Application.initArkLib() {
    app = this
}

fun computeId(size: Long, file: Path) = computeIdNative(size, file.toString())

fun createLinkFile(
    root: Path,
    title: String,
    desc: String,
    url: String,
    basePath: Path,
    downloadPreview: Boolean
) {
    return createLinkFileNative(
        root.toString(),
        title,
        desc,
        url,
        basePath.toString(),
        downloadPreview
    )
}

fun loadLinkFile(root: Path, file: Path): LinkData {
    return loadLinkFileNative(root.toString(), file.toString())
}

fun getLinkHash(url: String): String {
    return getLinkHashNative(url)
}

fun fetchLinkData(url: String): LinkData? {
    return fetchLinkDataNative(url)
}

fun pdfPreviewGenerate(path: String, previewQuality: PreviewQuality): Bitmap {
    return pdfPreviewGenerateNative(path, previewQuality.name)
}

// Initialize Rust Library Logging
external fun initRustLogger()

enum class PreviewQuality {
    HIGH, MEDIUM, LOW
}
