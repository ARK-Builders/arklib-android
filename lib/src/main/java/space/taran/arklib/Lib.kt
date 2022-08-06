package space.taran.arklib;

import android.graphics.Bitmap

private external fun createLinkFileNative(title: String, desc: String, url: String, basePath: String, downloadPreview: Boolean)
private external fun loadLinkFileNative(file_name: String): String
private external fun getLinkHashNative(url: String): String
private external fun pdfPreviewGenerateNative(path: String, quality: String): Bitmap


fun createLinkFile(title: String, desc: String, url: String, basePath: String, downloadPreview: Boolean) {
   return createLinkFileNative(title, desc, url, basePath, downloadPreview)
}

fun loadLinkFile(file_name: String): String {
   return loadLinkFileNative(file_name)
}

fun getLinkHash(url: String): String {
   return getLinkHashNative(url)
}

fun pdfPreviewGenerate(path: String, previewQuality: PreviewQuality): Bitmap {
   return pdfPreviewGenerateNative(path, previewQuality.name)
}

// Initialize Rust Library Logging
external fun initRustLogger()

enum class PreviewQuality {
    HIGH,MEDIUM,LOW
}
