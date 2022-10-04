package space.taran.arklib

import android.graphics.Bitmap
import java.nio.file.Path

data class LinkData(
   val title: String,
   val desc: String,
   val url: String,
   val imageUrl: String){
   companion object {
     @JvmStatic
     fun create(title: String, desc: String, url: String, imageUrl: String) : LinkData = LinkData(title, desc, url, imageUrl)
  }
}

private external fun computeIdNative(size: Long, file: String):Long
private external fun createLinkFileNative(title: String, desc: String, url: String, basePath: String, downloadPreview: Boolean)
private external fun loadLinkFileNative(file_name: String): String
private external fun loadLinkPreviewNative(file_name: String): ByteArray?
private external fun getLinkHashNative(url: String): String
private external fun fetchLinkDataNative(url: String): LinkData?
private external fun pdfPreviewGenerateNative(path: String, quality: String): Bitmap


fun computeId(size: Long, file: Path) = computeIdNative(size, file.toString())

fun createLinkFile(title: String, desc: String, url: String, basePath: String, downloadPreview: Boolean) {
   return createLinkFileNative(title, desc, url, basePath, downloadPreview)
}

fun loadLinkFile(file_name: String): String {
   return loadLinkFileNative(file_name)
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
fun loadLinkPreview(file_name: String): ByteArray? {
   return loadLinkPreviewNative(file_name)
}

// Initialize Rust Library Logging
external fun initRustLogger()

enum class PreviewQuality {
    HIGH,MEDIUM,LOW
}
