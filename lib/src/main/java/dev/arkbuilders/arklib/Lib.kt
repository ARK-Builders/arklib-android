package dev.arkbuilders.arklib

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
    val blake3: String
) : Parcelable {

    override fun toString() = blake3

    companion object {

        fun fromString(str: String) = create(str)

        @JvmStatic
        fun create(blake3: String): ResourceId =
            ResourceId(blake3)
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
